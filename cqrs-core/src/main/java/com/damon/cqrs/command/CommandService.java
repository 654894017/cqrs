package com.damon.cqrs.command;

import com.damon.cqrs.CqrsApplicationContext;
import com.damon.cqrs.cache.IAggregateCache;
import com.damon.cqrs.config.AggregateSlotLock;
import com.damon.cqrs.config.CqrsConfig;
import com.damon.cqrs.domain.AggregateRoot;
import com.damon.cqrs.domain.Command;
import com.damon.cqrs.domain.Event;
import com.damon.cqrs.event.EventCommittingContext;
import com.damon.cqrs.event.EventCommittingService;
import com.damon.cqrs.exception.*;
import com.damon.cqrs.snapshot.IAggregateSnapshootService;
import com.damon.cqrs.store.IEventStore;
import com.damon.cqrs.utils.GenericsUtils;
import com.damon.cqrs.utils.ReflectUtils;
import lombok.extern.slf4j.Slf4j;

import java.time.ZonedDateTime;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.locks.ReentrantLock;
import java.util.function.Function;
import java.util.function.Supplier;

import static com.google.common.base.Preconditions.checkNotNull;

/**
 * 聚合领域服务抽象类
 * <p>
 * 可以在此服务上封装dubbo、spring cloud 微服务框架。
 * <p>
 * 注意：负载均衡需要采用hash机制，建议使用一致性hash，当集群扩容、缩容时对聚合根的恢复影响较小。
 *
 * @author xianping_lu
 */
@Slf4j
public abstract class CommandService<T extends AggregateRoot> implements ICommandService<T> {
    /**
     * 聚合回溯等待超时时间
     */
    private final Long LOCK_WAITTING_TIME = 5000L;
    private final EventCommittingService eventCommittingService;
    private final IAggregateCache aggregateCache;
    private final IEventStore eventStore;
    private final IAggregateSnapshootService aggregateSnapshootService;
    private final AggregateSlotLock aggregateSlotLock;

    public CommandService(CqrsConfig cqrsConfig) {
        this.eventCommittingService = cqrsConfig.getEventCommittingService();
        this.aggregateCache = cqrsConfig.getAggregateCache();
        this.eventStore = cqrsConfig.getEventStore();
        this.aggregateSnapshootService = cqrsConfig.getAggregateSnapshootService();
        this.aggregateSlotLock = cqrsConfig.getAggregateSlotLock();
        CqrsApplicationContext.add(getAggregateType().getTypeName(), this);
    }

    @SuppressWarnings("unchecked")
    private Class<T> getAggregateType() {
        return GenericsUtils.getSuperClassGenricType(this.getClass(), 0);
    }

    private T load(final long aggregateId, final Class<T> aggregateType, Map<String, Object> shardingParams) {
        T aggregate = aggregateCache.get(aggregateId);
        if (aggregate != null) {
            log.debug("aggregate id: {}, aggreage type : {} from load local cache ", aggregateId, aggregate.getClass().getTypeName());
            return aggregate;
        }
        T snapshot = getAggregateSnapshot(aggregateId, aggregateType);
        if (snapshot != null) {
            List<List<Event>> events = eventStore.load(aggregateId, aggregateType, snapshot.getVersion() + 1, Integer.MAX_VALUE, shardingParams);
            events.forEach(event -> snapshot.replayEvents(event));
            aggregateCache.update(aggregateId, snapshot);
            log.info("aggregate id: {} , type: {} , event sourcing succeed. start version : {}, end version : {}.",
                    aggregateId, aggregateType, snapshot.getVersion() + 1, Integer.MAX_VALUE
            );
            return snapshot;
        } else {
            List<List<Event>> events = eventStore.load(aggregateId, aggregateType, 1, Integer.MAX_VALUE, shardingParams);
            T instance = ReflectUtils.newInstance(aggregateType, aggregateId);
            instance.setId(aggregateId);
            events.forEach(event -> instance.replayEvents(event));
            aggregateCache.update(aggregateId, instance);
            log.info("aggregate id: {} , type: {} , event sourcing succeed. start version : {}, end version : {}.",
                    aggregateId, aggregateType, 1, Integer.MAX_VALUE
            );
            return instance;
        }
    }

    /**
     * 聚合根初始化处理
     *
     * @param command
     * @param supplier
     * @param lockWaitingTime 聚合根更新冲突时间，会暂停当前聚合根新的command的处理，直到聚合根恢复完成时才接受新的command。
     * @throws AggregateEventConflictException   出现此异常的原因是当前聚合根在多个实例中存在（集群扩容时），可以捕获此异常然后重新在client发起调用，当前的请求会负载到新的实例上。
     * @throws AggregateCommandConflictException 重复的commanid导致出现该异常，出现在重复发送command的情况。
     * @throws EventStoreException               持久化事件时出现预料之外的错误。
     */
    @Override
    public CompletableFuture<T> process(final Command command, final Supplier<T> supplier, Long lockWaitingTime) {
        checkNotNull(supplier);
        T aggregate = supplier.get();
        checkNotNull(aggregate);
        checkNotNull(command);
        checkNotNull(aggregate.getId());
        checkNotNull(command.getCommandId());
        ReentrantLock lock = aggregateSlotLock.getLock(aggregate.getId());
        boolean flag;
        try {
            flag = lock.tryLock(lockWaitingTime, TimeUnit.MILLISECONDS);
        } catch (InterruptedException e) {
            CompletableFuture<T> exceptionFuture = new CompletableFuture<>();
            exceptionFuture.completeExceptionally(e);
            return exceptionFuture;
        }
        if (!flag) {
            String message = "aggregate id : %s , aggregate type: %s , processing timeout .";
            CompletableFuture<T> exceptionFuture = new CompletableFuture<>();
            exceptionFuture.completeExceptionally(new AggregateProcessingTimeoutException(
                    String.format(message, aggregate.getId(), aggregate.getClass().getTypeName())
            ));
            return exceptionFuture;
        }
        try {
            return commitDomainEventAsync(command.getCommandId(), aggregate, command.getShardingParams())
                    .thenCompose(__ -> CompletableFuture.completedFuture(aggregate));
        } catch (Throwable e) {
            CompletableFuture<T> exceptionFuture = new CompletableFuture<>();
            exceptionFuture.completeExceptionally(e);
            return exceptionFuture;
        } finally {
            lock.unlock();
        }

    }

    /**
     * 聚合根业务处理
     *
     * @param command
     * @param function
     * @param lockWaitingTime 聚合根更新冲突时间，会暂停当前聚合根新的command的处理，直到聚合根恢复完成时才接受新的command。
     * @return
     * @throws AggregateEventConflictException     出现此异常的原因是当前聚合根在多个实例中存在（集群扩容时），可以捕获此异常然后重新在client发起调用，当前的请求会负载到新的实例上。
     * @throws AggregateCommandConflictException   重复的commanid导致出现该异常，出现在重复发送command的情况。
     * @throws EventStoreException                 持久化事件时出现预料之外的错误。
     * @throws AggregateProcessingTimeoutException 聚合根更新冲突时间，会暂停当前聚合根新的command的处理，如果超过lockWaitingTime时间还未执行，会抛出此异常。
     * @throws AggregateNotFoundException
     */
    @Override
    public <R> CompletableFuture<R> process(final Command command, final Function<T, R> function, Long lockWaitingTime) {
        checkNotNull(command);
        checkNotNull(command.getAggregateId());
        long aggregateId = command.getAggregateId();
        ReentrantLock lock = aggregateSlotLock.getLock(command.getAggregateId());
        boolean flag;
        try {
            flag = lock.tryLock(lockWaitingTime, TimeUnit.MILLISECONDS);
        } catch (InterruptedException e) {
            String message = "aggregate id : %s , aggregate type: %s , processing timeout .";
            CompletableFuture<R> exceptionFuture = new CompletableFuture<>();
            exceptionFuture.completeExceptionally(new AggregateProcessingTimeoutException(
                    String.format(message, aggregateId, getAggregateType().getTypeName()), e
            ));
            return exceptionFuture;
        }

        if (!flag) {
            String message = "aggregate id : %s , aggregate type: %s , processing timeout .";
            CompletableFuture<R> exceptionFuture = new CompletableFuture<>();
            exceptionFuture.completeExceptionally(new AggregateProcessingTimeoutException(String.format(message, aggregateId, getAggregateType().getTypeName())));
            return exceptionFuture;
        }
        try {
            T aggregate = load(aggregateId, this.getAggregateType(), command.getShardingParams());
            if (aggregate == null) {
                throw new AggregateNotFoundException(aggregateId);
            }
            R result = function.apply(aggregate);
            if (aggregate.getChanges().isEmpty()) {
                return CompletableFuture.completedFuture(result);
            } else {
                return commitDomainEventAsync(command.getCommandId(), aggregate, command.getShardingParams())
                        .thenCompose(__ -> CompletableFuture.completedFuture(result));
            }
        } catch (Throwable e) {
            CompletableFuture<R> exceptionFuture = new CompletableFuture<>();
            exceptionFuture.completeExceptionally(e);
            return exceptionFuture;
        } finally {
            lock.unlock();
        }
    }

    /**
     * 聚合根初始化 + 聚合根业务处理原子操作，主要用在部分场景，需要先初始化聚合根，然后再处理业务。
     *
     * @param command
     * @param supplier
     * @param function
     * @param lockWaitingTime
     * @return
     */
    public <R> CompletableFuture<R> process(final Command command, final Supplier<T> supplier, final Function<T, R> function, Long lockWaitingTime) {
        checkNotNull(command);
        checkNotNull(command.getAggregateId());
        long aggregateId = command.getAggregateId();
        ReentrantLock lock = aggregateSlotLock.getLock(command.getAggregateId());
        boolean flag;
        try {
            flag = lock.tryLock(lockWaitingTime, TimeUnit.MILLISECONDS);
        } catch (InterruptedException e) {
            String message = "aggregate id : %s , aggregate type: %s , processing timeout .";
            CompletableFuture<R> exceptionFuture = new CompletableFuture<>();
            exceptionFuture.completeExceptionally(new AggregateProcessingTimeoutException(
                    String.format(message, aggregateId, getAggregateType().getTypeName()), e
            ));
            return exceptionFuture;
        }

        if (!flag) {
            String message = "aggregate id : %s , aggregate type: %s , processing timeout .";
            CompletableFuture<R> exceptionFuture = new CompletableFuture<>();
            exceptionFuture.completeExceptionally(new AggregateProcessingTimeoutException(String.format(message, aggregateId, getAggregateType().getTypeName())));
            return exceptionFuture;
        }
        try {
            T aggregate = this.load(aggregateId, this.getAggregateType(), command.getShardingParams());
            if (aggregate == null) {
                aggregate = supplier.get();
            }
            R result = function.apply(aggregate);
            if (aggregate.getChanges().isEmpty()) {
                return CompletableFuture.completedFuture(result);
            } else {
                if (aggregate == null) {
                    //必须等待事件持久化，在返回result。
                    commitDomainEventAsync(command.getCommandId(), aggregate, command.getShardingParams()).join();
                    return CompletableFuture.completedFuture(result);
                } else {
                    return commitDomainEventAsync(command.getCommandId(), aggregate, command.getShardingParams())
                            .thenCompose(__ -> CompletableFuture.completedFuture(result));
                }
            }
        } catch (Throwable e) {
            CompletableFuture<R> exceptionFuture = new CompletableFuture<>();
            exceptionFuture.completeExceptionally(e);
            return exceptionFuture;
        } finally {
            lock.unlock();
        }
    }

    public <R> CompletableFuture<R> process(final Command command, final Function<T, R> function) {
        return this.process(command, function, LOCK_WAITTING_TIME);
    }

    public CompletableFuture<T> process(final Command command, final Supplier<T> supplier) {
        return this.process(command, supplier, LOCK_WAITTING_TIME);
    }

    public <R> CompletableFuture<R> process(final Command command, final Supplier<T> create, final Function<T, R> updateFunction) {
        return this.process(command, create, updateFunction, LOCK_WAITTING_TIME);
    }

    private CompletableFuture<Void> commitDomainEventAsync(long commandId, T aggregate, Map<String, Object> shardingParams) {
        CompletableFuture<Boolean> future = new CompletableFuture<>();
        EventCommittingContext context = EventCommittingContext.builder()
                .aggregateId(aggregate.getId())
                .aggregateTypeName(aggregate.getClass().getTypeName())
                .events(aggregate.getChanges())
                .shardingParams(shardingParams)
                .commandId(commandId)
                .future(future)
                .build();
        aggregate.acceptChanges();
        context.setVersion(aggregate.getVersion());
        if (aggregate.isSnapshotCycle(snapshotCycle())) {
            T snapsot = this.createAggregateSnapshot(aggregate);
            context.setSnapshot(snapsot);
            log.debug("aggreaget id : {}, type : {}, version : {}, create snapshhot succeed.", snapsot.getId(), snapsot.getClass().getTypeName(), snapsot.getVersion());
        }
        eventCommittingService.commitDomainEventAsync(context);
        return future.thenAccept(__ -> {
            if (aggregateCache.get(aggregate.getId()) == null) {
                aggregateCache.update(aggregate.getId(), aggregate);
            }

            if (context.getSnapshot() != null) {
                aggregateSnapshootService.saveAggregateSnapshot(context.getSnapshot());
                aggregate.setLastSnapTimestamp(ZonedDateTime.now());
            }
        });
    }

}
