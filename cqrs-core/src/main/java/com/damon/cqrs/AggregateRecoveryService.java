package com.damon.cqrs;

import com.damon.cqrs.domain.AggregateRoot;
import com.damon.cqrs.event.CQRSContext;
import com.damon.cqrs.store.IEventStore;
import com.damon.cqrs.utils.AggregateLockUtils;
import com.damon.cqrs.utils.ReflectUtils;
import com.damon.cqrs.utils.ThreadUtils;
import lombok.extern.slf4j.Slf4j;

import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.locks.ReentrantLock;
@Slf4j
public class AggregateRecoveryService {

    private IEventStore eventStore;

    private IAggregateCache aggregateCache;

    public AggregateRecoveryService(IEventStore eventStore, IAggregateCache aggregateCache){
        this.eventStore = eventStore;
        this.aggregateCache =  aggregateCache;
    }

    public <T extends  AggregateRoot> void recoverAggregate(Long aggregateId, String aggregateType, Map<String, Object> shardingParams) {
        ReentrantLock lock = AggregateLockUtils.getLock(aggregateId);
        AbstractDomainService<T>  domainService = CQRSContext.get(aggregateType);
        lock.lock();
        for (; ; ) {
            try {
                Class<T> aggregateClass = ReflectUtils.getClass(aggregateType);
                boolean success = domainService.getAggregateSnapshot(aggregateId, aggregateClass).thenCompose(snapshoot -> {
                    if (snapshoot != null) {
                        return this.sourcingEvent(snapshoot, snapshoot.getVersion() + 1, Integer.MAX_VALUE, shardingParams);
                    } else {
                        T instance = ReflectUtils.newInstance(ReflectUtils.getClass(aggregateType));
                        instance.setId(aggregateId);
                        return this.sourcingEvent(instance, 1, Integer.MAX_VALUE, shardingParams);
                    }
                }).exceptionally(ex -> {
                    log.error("aggregate id: {} , type: {} , event sourcing failed. ", aggregateId, aggregateType, ex);
                    ThreadUtils.sleep(1000);
                    return false;
                }).join();
                if (success) {
                    break;
                }
            } catch (Throwable e) {
                log.error("aggregate id: {} , type: {} , event sourcing failed. ", aggregateId, aggregateType, e);
                ThreadUtils.sleep(1000);
            } finally {
                lock.unlock();
            }
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

        log.info("aggregate id: {} , type: {} , start event sourcing. start version : {}, end version : {}.",
                aggregate.getId(), aggregate.getClass().getTypeName(), startVersion, endVersion);

        return eventStore.load(aggregate.getId(), aggregate.getClass(), startVersion, endVersion, shardingParams).thenApply(events -> {
            events.forEach(es -> aggregate.replayEvents(es));
            aggregateCache.update(aggregate.getId(), aggregate);
            log.info("aggregate id: {} , type: {} , event sourcing succeed. start version : {}, end version : {}.",
                    aggregate.getId(), aggregate.getClass().getTypeName(), startVersion, endVersion);
            return true;
        }).whenComplete((v, e) -> {
            if (e != null) {
                log.error("aggregate id: {} , type: {} , event sourcing failed. start version : {}, end version : {}.",
                        aggregate.getId(), aggregate.getClass().getTypeName(), startVersion, endVersion, e);
            }
        });
    }

}
