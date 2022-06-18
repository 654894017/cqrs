package com.damon.cqrs;

import com.damon.cqrs.domain.AggregateRoot;
import com.damon.cqrs.domain.Command;
import com.damon.cqrs.event.CQRSContext;
import com.damon.cqrs.event.EventCommittingContext;
import com.damon.cqrs.event.EventCommittingService;
import com.damon.cqrs.exception.*;
import com.damon.cqrs.store.IEventStore;
import com.damon.cqrs.utils.*;
import lombok.extern.slf4j.Slf4j;

import java.time.ZonedDateTime;
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
public abstract class AbstractDomainService<T extends AggregateRoot> {

    /**
     * 聚合回溯等待超时时间
     */
    private final int LOCK_WAITTING_TIME = 5;
    private final EventCommittingService eventCommittingService;
    private final IAggregateCache aggregateCache;
    private final IEventStore eventStore;
    private final IAggregateSnapshootService aggregateSnapshootService;
    private final IBeanCopy beanCopy;

    public AbstractDomainService(CQRSConfig config) {
        this.eventCommittingService = config.getEventCommittingService();
        this.aggregateCache = config.getAggregateCache();
        this.eventStore = config.getEventStore();
        this.aggregateSnapshootService = config.getAggregateSnapshootService();
        this.beanCopy = config.getBeanCopy();
        CQRSContext.add(getAggregateType().getTypeName(), this);
    }

    @SuppressWarnings("unchecked")
    private Class<T> getAggregateType() {
        return GenericsUtils.getSuperClassGenricType(this.getClass(), 0);
    }

    private CompletableFuture<T> load(final long aggregateId, final Class<T> aggregateType, Map<String, Object> shardingParams) {
        T aggregate = aggregateCache.get(aggregateId);
        if (aggregate != null) {
            log.debug("aggregate id: {}, aggreage type : {} from load local cache ", aggregateId, aggregate.getClass().getTypeName());
            return CompletableFuture.completedFuture(aggregate);
        }
        return getAggregateSnapshot(aggregateId, aggregateType).exceptionally((e) -> {
            log.error("get aggregate snapshoot failed , aggregate id: {} , type: {}. ", aggregateId, aggregateType.getTypeName(), e
            );
            return null;
        }).thenCompose(snapshot -> {
            if (snapshot != null) {
                return eventStore.load(aggregateId, aggregateType, snapshot.getVersion() + 1, Integer.MAX_VALUE, shardingParams)
                        .thenApply(events -> {
                            events.forEach(event -> snapshot.replayEvents(event));
                            aggregateCache.update(aggregateId, snapshot);
                            log.info(
                                    "aggregate id: {} , type: {} , event sourcing succeed. start version : {}, end version : {}.",
                                    aggregateId, aggregateType, snapshot.getVersion() + 1, Integer.MAX_VALUE
                            );
                            return snapshot;
                        }).whenComplete((a, e) -> {
                            if (e != null) {
                                log.error(
                                        "aggregate id: {} , type: {} , event sourcing failed. start version : {}, end version : {}.",
                                        aggregateId, aggregateType.getTypeName(), snapshot.getVersion() + 1, Integer.MAX_VALUE, e
                                );
                            }
                        });
            } else {
                return eventStore.load(aggregateId, aggregateType, 1, Integer.MAX_VALUE, shardingParams).thenApply(events -> {
                    if (events.isEmpty()) {
                        return null;
                    }
                    T instance = ReflectUtils.newInstance(aggregateType);
                    instance.setId(aggregateId);
                    events.forEach(event -> instance.replayEvents(event));
                    aggregateCache.update(aggregateId, instance);
                    log.info(
                            "aggregate id: {} , type: {} , event sourcing succeed. start version : {}, end version : {}.",
                            aggregateId, aggregateType, 1, Integer.MAX_VALUE
                    );
                    return instance;
                }).whenComplete((a, e) -> {
                    if (e != null) {
                        log.error("aggregate id: {} , type: {} , event sourcing failed. start version : {}, end version : {}.",
                                aggregateId, aggregateType.getTypeName(), 1, Integer.MAX_VALUE, e
                        );
                    }
                });
            }
        });
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
    protected CompletableFuture<T> process(final Command command, final Supplier<T> supplier, int lockWaitingTime) {
        checkNotNull(supplier);
        T aggregate = supplier.get();
        checkNotNull(aggregate);
        checkNotNull(command);
        checkNotNull(aggregate.getId());
        checkNotNull(command.getCommandId());
        ReentrantLock lock = AggregateLockUtils.getLock(aggregate.getId());
        boolean flag;
        try {
            flag = lock.tryLock(lockWaitingTime, TimeUnit.SECONDS);
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
            return commitDomainEventAsync(command.getCommandId(), aggregate, command.getShardingParams()).thenApply(__ -> aggregate);
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
    protected <R> CompletableFuture<R> process(final Command command, final Function<T, R> function, int lockWaitingTime) {
        checkNotNull(command);
        checkNotNull(command.getAggregateId());
        long aggregateId = command.getAggregateId();
        ReentrantLock lock = AggregateLockUtils.getLock(command.getAggregateId());
        boolean flag;
        try {
            flag = lock.tryLock(lockWaitingTime, TimeUnit.SECONDS);
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
            return this.load(aggregateId, this.getAggregateType(), command.getShardingParams()).thenCompose(aggregate -> {
                if (aggregate == null) {
                    throw new AggregateNotFoundException(aggregateId);
                }
                R result = function.apply(aggregate);
                if (aggregate.getChanges().isEmpty()) {
                    return CompletableFuture.completedFuture(result);
                } else {
                    return commitDomainEventAsync(command.getCommandId(), aggregate, command.getShardingParams()).thenCompose(__ -> CompletableFuture.completedFuture(result));
                }
            });
        } finally {
            lock.unlock();
        }
    }

    protected <R> CompletableFuture<R> process(final Command command, final Function<T, R> function) {
        return this.process(command, function, LOCK_WAITTING_TIME);
    }

    protected CompletableFuture<T> process(final Command command, final Supplier<T> supplier) {
        return this.process(command, supplier, LOCK_WAITTING_TIME);
    }

    private CompletableFuture<Void> commitDomainEventAsync(long commandId, T aggregate, Map<String, Object> shardingParams) {
        CompletableFuture<Boolean> future = new CompletableFuture<>();
        EventCommittingContext context = EventCommittingContext.builder()
                .aggregateId(aggregate.getId())
                .aggregateTypeName(aggregate.getClass().getTypeName())
                .events(aggregate.getChanges())
                .shardingParams(shardingParams)
                .commandId(commandId)
                .future(future).build();
        aggregate.acceptChanges();
        context.setVersion(aggregate.getVersion());
        long second = DateUtils.getSecond(aggregate.getLastSnapshootTimestamp(), aggregate.getTimestamp());
        // 开启聚合快照且达到快照创建周期
        if (aggregate.createSnapshotCycle() > 0 && !aggregate.getOnSnapshoot() && second > aggregate.createSnapshotCycle()) {
            T snapsot = ReflectUtils.newInstance(aggregate.getClass());
            beanCopy.copy(aggregate, snapsot);
            context.setSnapshot(snapsot);
            aggregate.setOnSnapshoot(true);
            log.info("aggreaget id : {}, type : {}, version : {}, create snapshhot succeed.", snapsot.getId(), snapsot.getClass().getTypeName(), snapsot.getVersion());
        }
        eventCommittingService.commitDomainEventAsync(context);
        return future.thenAccept(__ -> {
            if (aggregateCache.get(aggregate.getId()) == null) {
                aggregateCache.update(aggregate.getId(), aggregate);
            }

            if (context.getSnapshot() != null) {
                aggregateSnapshootService.saveAggregategetSnapshot(context.getSnapshot());
                aggregate.setLastSnapshootTimestamp(ZonedDateTime.now());
                aggregate.setOnSnapshoot(false);
            }
        });
    }

    /**
     * 获取聚合快照，用于加速聚合回溯(对于聚合存在的生命周期特别长且修改特别频繁时需要实现)
     * <p>
     * 逻辑：先判断是否存在聚合快照，如果不存在聚合快照，从Q端恢复聚合。
     *
     * @param aggregateId
     * @param classes
     * @return
     */
    public abstract CompletableFuture<T> getAggregateSnapshot(long aggregateId, Class<T> classes);

    /**
     * 保存聚合快照
     * <p>
     * 可以保存到类似redis高性能缓存中（不用担心丢失，Q端、Event库中都存有聚合数据信息）
     *
     * @param aggregate
     * @return
     */
    public abstract CompletableFuture<Boolean> saveAggregateSnapshot(T aggregate);


}
