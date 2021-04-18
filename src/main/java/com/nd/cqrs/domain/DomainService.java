package com.nd.cqrs.domain;

import static com.google.common.base.Preconditions.checkNotNull;

import java.util.concurrent.CompletableFuture;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.locks.ReentrantLock;
import java.util.function.Consumer;
import java.util.function.Supplier;

//import org.apache.commons.beanutils.BeanUtils;

import com.nd.cqrs.AggregateLock;
import com.nd.cqrs.AggregateOfDomainServiceMap;
import com.nd.cqrs.EventCommittingContext;
import com.nd.cqrs.EventCommittingService;
import com.nd.cqrs.IEventStore;
import com.nd.cqrs.exception.AggregateCommandConflictException;
import com.nd.cqrs.exception.AggregateEventConflictException;
import com.nd.cqrs.exception.AggregateNotFoundException;
import com.nd.cqrs.exception.AggregateProcessingTimeoutException;
import com.nd.cqrs.exception.EventStoreException;
import com.nd.cqrs.utils.BeanMapper;
import com.nd.cqrs.utils.GenericsUtils;
import com.nd.cqrs.utils.ReflectUtis;

import lombok.extern.slf4j.Slf4j;

/**
 * 聚合领域服务抽象类
 * 
 * 可以在此服务上封装dubbo、spring cloud 微服务框架。
 * 
 * 注意：负载均衡需要采用hash机制，建议使用一致性hash，当集群扩容、缩容时对聚合根的恢复影响较小。
 * 
 * 
 * @author xianping_lu
 *
 */
@Slf4j
public abstract class DomainService<T extends Aggregate> {

    private EventCommittingService eventCommittingService;
    /**
     * 聚合回溯等待超时时间
     */
    private final int LOCK_WAITTING_TIME = 5;

    @SuppressWarnings("unchecked")
    private Class<T> getAggregateType() {
        return GenericsUtils.getSuperClassGenricType(this.getClass(), 0);
    }

    public DomainService(EventCommittingService eventCommittingService) {
        this.eventCommittingService = checkNotNull(eventCommittingService);
        AggregateOfDomainServiceMap.add(getAggregateType().getTypeName(), this);
    }

    private CompletableFuture<T> load(final long aggregateId, final Class<T> aggregateType) {
        T aggregate = eventCommittingService.getAggregateCache().get(aggregateId);
        if (aggregate != null) {
            log.debug("aggregate id: {}, aggreage type : {} from load local cache ", aggregateId, aggregate.getClass().getTypeName());
            return CompletableFuture.completedFuture(aggregate);
        }
        return getAggregateSnapshoot(aggregateId, aggregateType).thenCompose(snapshoot -> {
            IEventStore eventStore = eventCommittingService.getEventStore();
            if (snapshoot != null) {
                return eventStore.load(aggregateId, aggregateType, snapshoot.getVersion() + 1, Integer.MAX_VALUE).thenApply(events -> {
                    events.forEach(event -> snapshoot.replayEvents(event));
                    eventCommittingService.getAggregateCache().updateAggregateCache(aggregateId, snapshoot);
                    return snapshoot;
                }).whenComplete((a, e) -> {
                    if (e != null) {
                        log.error("aggregate id: {} , type: {} , event sourcing failutre. start version : {}, end version : {}.", //
                                aggregateId, aggregateType.getTypeName(), snapshoot.getVersion() + 1, Integer.MAX_VALUE, e);
                    }
                });
            } else {
                return eventStore.load(aggregateId, aggregateType, 1, Integer.MAX_VALUE).thenApply(events -> {
                    if (events.isEmpty()) {
                        return null;
                    }
                    T aggregateInstance = ReflectUtis.newInstance(aggregateType);
                    aggregateInstance.setId(aggregateId);
                    events.forEach(event -> aggregateInstance.replayEvents(event));
                    eventCommittingService.getAggregateCache().updateAggregateCache(aggregateId, aggregateInstance);
                    return aggregateInstance;
                }).whenComplete((a, e) -> {
                    if (e != null) {
                        log.error("aggregate id: {} , type: {} , event sourcing failutre. start version : {}, end version : {}.", //
                                aggregateId, aggregateType.getTypeName(), 1, Integer.MAX_VALUE, e);
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
        ReentrantLock lock = AggregateLock.getLock(aggregate.getId());
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
            exceptionFuture.completeExceptionally(new AggregateProcessingTimeoutException(String.format(message, aggregate.getId(), aggregate.getClass().getTypeName())));
            return exceptionFuture;
        }
        try {
            long aggregateId = aggregate.getId();
            return commitDomainEventAsync(command.getCommandId(), aggregate).whenComplete((a, e) -> {
                if (e == null) {
                    eventCommittingService.getAggregateCache().updateAggregateCache(aggregateId, aggregate);
                }
            });
        } catch (Throwable e) {
            CompletableFuture<T> future = new CompletableFuture<>();
            future.completeExceptionally(e);
            return future;
        } finally {
            lock.unlock();
        }

    }

    /**
     * 聚合根业务处理
     * 
     * @param command
     * @param consumer
     * @param lockWaitingTime 聚合根更新冲突时间，会暂停当前聚合根新的command的处理，直到聚合根恢复完成时才接受新的command。
     * @return
     * @throws AggregateEventConflictException     出现此异常的原因是当前聚合根在多个实例中存在（集群扩容时），可以捕获此异常然后重新在client发起调用，当前的请求会负载到新的实例上。
     * @throws AggregateCommandConflictException   重复的commanid导致出现该异常，出现在重复发送command的情况。
     * @throws EventStoreException                 持久化事件时出现预料之外的错误。
     * @throws AggregateProcessingTimeoutException 聚合根更新冲突时间，会暂停当前聚合根新的command的处理，如果超过lockWaitingTime时间还未执行，会抛出此异常。
     * @throws AggregateNotFoundException
     */
    protected CompletableFuture<T> process(final Command command, final Consumer<T> consumer, int lockWaitingTime) {
        checkNotNull(command);
        checkNotNull(command.getAggregateId());
        long aggregateId = command.getAggregateId();
        ReentrantLock lock = AggregateLock.getLock(command.getAggregateId());
        boolean flag;
        try {
            flag = lock.tryLock(lockWaitingTime, TimeUnit.SECONDS);
        } catch (InterruptedException e) {
            String message = "aggregate id : %s , aggregate type: %s , processing timeout .";
            CompletableFuture<T> exceptionFuture = new CompletableFuture<>();
            exceptionFuture.completeExceptionally(new AggregateProcessingTimeoutException(String.format(message, aggregateId, getAggregateType().getTypeName()), e));
            return exceptionFuture;
        }

        if (!flag) {
            String message = "aggregate id : %s , aggregate type: %s , processing timeout .";
            CompletableFuture<T> exceptionFuture = new CompletableFuture<>();
            exceptionFuture.completeExceptionally(new AggregateProcessingTimeoutException(String.format(message, aggregateId, getAggregateType().getTypeName())));
            return exceptionFuture;
        }
        try {
            return load(aggregateId, this.getAggregateType()).thenCompose(aggregate -> {
                if (aggregate == null) {
                    throw new AggregateNotFoundException(aggregateId);
                }
                consumer.accept(aggregate);
                return commitDomainEventAsync(command.getCommandId(), aggregate);
            });
        } finally {
            lock.unlock();
        }

    }

    protected CompletableFuture<T> process(final Command command, final Consumer<T> consumer) {
        return this.process(command, consumer, LOCK_WAITTING_TIME);
    }

    protected CompletableFuture<T> process(final Command command, final Supplier<T> supplier) {
        return this.process(command, supplier, LOCK_WAITTING_TIME);
    }

    private CompletableFuture<T> commitDomainEventAsync(long commandId, T aggregate) {
        CompletableFuture<Boolean> future = new CompletableFuture<>();
        EventCommittingContext context = EventCommittingContext.builder().events(aggregate.getChanges()).commandId(commandId).future(future).build();
        aggregate.acceptChanges();
        context.setVersion(aggregate.getVersion());
        T temp = ReflectUtis.newInstance(aggregate.getClass());
        BeanMapper.map(aggregate, temp);
        context.setAggregateSnapshoot(temp);
        eventCommittingService.commitDomainEventAsync(context);
        return future.thenApply(success -> temp);

    }

    /**
     * 获取聚合快照，用于加速聚合回溯(对于聚合存在的生命周期特别长且修改特别频繁时需要实现)
     * 
     * 逻辑：先判断是否存在聚合快照，如果不存在聚合快照，从Q端恢复聚合。
     * 
     * @param aggregateId
     * @param classes
     * @return
     */
    public abstract CompletableFuture<T> getAggregateSnapshoot(long aggregateId, Class<T> classes);

    /**
     * 保存聚合快照
     * 
     * 可以保存到类似redis高性能缓存中（不用担心丢失，Q端、Event库中都存有聚合数据信息）
     * 
     * @param aggregate
     * @return
     */
    public abstract CompletableFuture<Boolean> saveAggregateSnapshoot(T aggregate);

}
