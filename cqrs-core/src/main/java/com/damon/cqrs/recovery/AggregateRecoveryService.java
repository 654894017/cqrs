package com.damon.cqrs.recovery;

import com.damon.cqrs.config.AggregateSlotLock;
import com.damon.cqrs.CqrsApplicationContext;
import com.damon.cqrs.cache.IAggregateCache;
import com.damon.cqrs.command.CommandService;
import com.damon.cqrs.domain.AggregateRoot;
import com.damon.cqrs.store.IEventStore;
import com.damon.cqrs.utils.ReflectUtils;
import com.damon.cqrs.utils.ThreadUtils;
import lombok.extern.slf4j.Slf4j;

import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.locks.ReentrantLock;

/**
 * 聚合根恢复服务
 */
@Slf4j
public class AggregateRecoveryService {
    private final IEventStore eventStore;
    private final IAggregateCache aggregateCache;
    private final AggregateSlotLock aggregateSlotLock;
    public AggregateRecoveryService(IEventStore eventStore, IAggregateCache aggregateCache, AggregateSlotLock aggregateSlotLock) {
        this.eventStore = eventStore;
        this.aggregateCache = aggregateCache;
        this.aggregateSlotLock = aggregateSlotLock;
    }
    public <T extends AggregateRoot> void recoverAggregate(Long aggregateId, String aggregateType, Map<String, Object> shardingParams) {
        ReentrantLock lock = aggregateSlotLock.getLock(aggregateId);
        CommandService<T> commandService = CqrsApplicationContext.get(aggregateType);
        lock.lock();
        try {
            for (; ; ) {
                Class<T> aggregateClass = ReflectUtils.getClass(aggregateType);
                boolean succeeded = commandService.getAggregateSnapshot(aggregateId, aggregateClass).thenCompose(snapshoot -> {
                    if (snapshoot != null) {
                        return this.sourcingEvent(snapshoot, snapshoot.getVersion() + 1, Integer.MAX_VALUE, shardingParams);
                    } else {
                        T instance = ReflectUtils.newInstance(ReflectUtils.getClass(aggregateType));
                        instance.setId(aggregateId);
                        return this.sourcingEvent(instance, 1, Integer.MAX_VALUE, shardingParams);
                    }
                }).exceptionally(ex -> {
                    log.error("event sourcing failed, aggregate id: {} , type: {}. ", aggregateId, aggregateType, ex);
                    ThreadUtils.sleep(5000);
                    return false;
                }).join();
                if (succeeded) {
                    break;
                }
            }
        } finally {
            lock.unlock();
        }
    }

    /**
     * 回溯事件
     *
     * @param aggregate
     * @param startVersion
     * @param endVersion
     * @return
     */
    private CompletableFuture<Boolean> sourcingEvent(AggregateRoot aggregate, int startVersion, int endVersion, Map<String, Object> shardingParams) {

        log.info("start event sourcing, aggregate id: {} , type: {}, start version : {}, end version : {}.",
                aggregate.getId(), aggregate.getClass().getTypeName(), startVersion, endVersion);

        return eventStore.load(aggregate.getId(), aggregate.getClass(), startVersion, endVersion, shardingParams).thenApply(events -> {
            events.forEach(es -> aggregate.replayEvents(es));
            aggregateCache.update(aggregate.getId(), aggregate);
            log.info("event sourcing succeed, aggregate id: {} , type: {}, start version : {}, end version : {}.",
                    aggregate.getId(), aggregate.getClass().getTypeName(), startVersion, endVersion);
            return true;
        }).exceptionally(e -> {
            log.error("event sourcing failed, aggregate id: {}, type: {}, start version : {}, end version : {}.",
                    aggregate.getId(), aggregate.getClass().getTypeName(), startVersion, endVersion, e);
            return false;
        });
    }

}
