package com.damon.cqrs.recovery;

import com.damon.cqrs.CqrsApplicationContext;
import com.damon.cqrs.cache.IAggregateCache;
import com.damon.cqrs.command.ICommandService;
import com.damon.cqrs.config.AggregateSlotLock;
import com.damon.cqrs.domain.AggregateRoot;
import com.damon.cqrs.domain.Event;
import com.damon.cqrs.exception.EventSourcingException;
import com.damon.cqrs.store.IEventStore;
import com.damon.cqrs.utils.ReflectUtils;
import com.damon.cqrs.utils.ThreadUtils;
import lombok.extern.slf4j.Slf4j;

import java.util.List;
import java.util.Map;
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
        ICommandService<T> commandService = CqrsApplicationContext.get(aggregateType);
        lock.lock();
        try {
            for (; ; ) {
                try {
                    Class<T> aggregateClass = ReflectUtils.getClass(aggregateType);
                    T snapshot = commandService.getAggregateSnapshot(aggregateId, aggregateClass);
                    if (snapshot != null) {
                        this.sourcingEvent(snapshot, snapshot.getVersion() + 1, Integer.MAX_VALUE, shardingParams);
                    } else {
                        T instance = ReflectUtils.newInstance(ReflectUtils.getClass(aggregateType), aggregateId);
                        instance.setId(aggregateId);
                        this.sourcingEvent(instance, 1, Integer.MAX_VALUE, shardingParams);
                    }
                    break;
                } catch (Throwable e) {
                    log.error("event sourcing failed, aggregate id: {} , type: {}. ", aggregateId, aggregateType, e);
                    ThreadUtils.sleep(5000);
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
    private void sourcingEvent(AggregateRoot aggregate, int startVersion, int endVersion, Map<String, Object> shardingParams) {

        log.info("start event sourcing, aggregate id: {} , type: {}, start version : {}, end version : {}.",
                aggregate.getId(), aggregate.getClass().getTypeName(), startVersion, endVersion);
        try {
            long loadStartTime = System.currentTimeMillis();
            List<List<Event>> events = eventStore.load(aggregate.getId(), aggregate.getClass(), startVersion, endVersion, shardingParams);
            log.info("aggregate id: {} , type: {}, start version : {}, end version : {}, load costTime : {}",
                    aggregate.getId(), aggregate.getClass().getTypeName(), startVersion, Integer.MAX_VALUE, System.currentTimeMillis() - loadStartTime);
            long replayStartTime = System.currentTimeMillis();
            events.forEach(es -> aggregate.replayEvents(es));
            log.info("aggregate id: {} , type: {}, start version : {}, end version : {}, replay costTime : {}",
                    aggregate.getId(), aggregate.getClass().getTypeName(), startVersion, Integer.MAX_VALUE, System.currentTimeMillis() - replayStartTime);
            aggregateCache.update(aggregate.getId(), aggregate);
            log.info("event sourcing succeed, aggregate id: {} , type: {}, start version : {}, end version : {}.",
                    aggregate.getId(), aggregate.getClass().getTypeName(), startVersion, endVersion);
        } catch (Throwable e) {
            String message = String.format("event sourcing failed, aggregate id: %s, type: %s, start version: %s, end version: %s.",
                    aggregate.getId(), aggregate.getClass().getTypeName(), startVersion, endVersion, e);
            throw new EventSourcingException(message, e);
        }
    }

}
