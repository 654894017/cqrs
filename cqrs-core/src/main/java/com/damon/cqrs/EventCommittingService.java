package com.damon.cqrs;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.locks.ReentrantLock;
import java.util.stream.Collectors;

import com.damon.cqrs.utils.ReflectUtils;
import com.damon.cqrs.utils.ThreadUtils;
import com.domain.cqrs.domain.Aggregate;

import lombok.extern.slf4j.Slf4j;

/**
 * 领域事件提交服务
 * 
 * @author xianping_lu
 *
 */
@Slf4j
public class EventCommittingService {
    private final List<EventCommittingMailBox> mailBoxs;
    private final ExecutorService service;
    private final IEventStore eventStore;
    private final int mailboxNumber;
    private IAggregateSnapshootService aggregateSnapshootService;
    private IAggregateCache aggregateCache;
    private final String SOURCING_EVENT_MESSAGE = "aggregate id: {} , type: {} , start event sourcing. start version : {}, end version : {}.";
    private final String SOURCING_EVENT_SUCCESS_MESSAGE = "aggregate id: {} , type: {} , event sourcing sucess. start version : {}, end version : {}.";
    private final String SOURCING_EVENT_FAILTURE_MESSAGE = "aggregate id: {} , type: {} , event sourcing failutre. start version : {}, end version : {}.";

    public EventCommittingService(IEventStore eventStore, IAggregateSnapshootService aggregateSnapshootService, IAggregateCache aggregateCache, int mailBoxNumber, int batchSize) {
        this.mailBoxs = new ArrayList<EventCommittingMailBox>(mailBoxNumber);
        this.service = Executors.newFixedThreadPool(mailBoxNumber);
        this.mailboxNumber = mailBoxNumber;
        this.eventStore = eventStore;
        this.aggregateSnapshootService = aggregateSnapshootService;
        this.aggregateCache = aggregateCache;
        for (int number = 0; number < mailBoxNumber; number++) {
            mailBoxs.add(new EventCommittingMailBox(service, contexts -> batchStorageEvent(contexts), number, batchSize));
        }
    }

    /**
     * 提交聚合事件
     * 
     * @param event
     */
    public void commitDomainEventAsync(EventCommittingContext event) {
        int index = (int) (Math.abs(event.getAggregateSnapshoot().getId()) % mailboxNumber);
        EventCommittingMailBox maxibox = mailBoxs.get(index);
        maxibox.enqueue(event);
    }

    /**
     * 批量保存聚合事件
     * 
     * @param <T>
     * @param contexts
     */
    private <T extends Aggregate> void batchStorageEvent(List<EventCommittingContext> contexts) {
        List<DomainEventStream> eventStream = contexts.stream().map(context -> {
            AggregateGroup group = AggregateGroup.builder().aggregateId(context.getAggregateSnapshoot().getId())//
                    .aggregateType(context.getAggregateSnapshoot().getClass().getTypeName()).maiBox(context.getMailBox()).build();
            return DomainEventStream.builder().aggregateSnapshoot(context.getAggregateSnapshoot()).future(context.getFuture()).commandId(context.getCommandId()).group(group)
                    .events(context.getEvents()).version(context.getVersion()).build();
        }).collect(Collectors.toList());
        Map<AggregateGroup, List<DomainEventStream>> map = eventStream.stream().collect(Collectors.groupingBy(stream -> stream.getGroup()));
        eventStore.store(map).thenAccept(results -> {
            results.forEach(result -> {
                AggregateGroup group = result.getGroup();
                List<DomainEventStream> aggregateGroup = map.get(group);
                if (EventAppendStatus.Success.equals(result.getEventAppendStatus())) {
                    DomainEventStream stream = aggregateGroup.get(aggregateGroup.size() - 1);
                    Aggregate aggregateSnapshoot = stream.getAggregateSnapshoot();
                    aggregateSnapshootService.addAggregategetSnapshoot(aggregateSnapshoot);
                    aggregateGroup.forEach(context -> context.getFuture().complete(true));
                } else {
                    DomainService<T> domainService = AggregateOfDomainServiceMap.get(group.getAggregateType());
                    // 当聚合事件保存冲突时，同时也需要锁住领域服务不能让新的Command进入领域服务，不然聚合回溯的聚合实体是不正确的，由业务调用方重新发起请求
                    ReentrantLock lock = AggregateLock.getLock(group.getAggregateId());
                    lock.lock();
                    try {
                        // 清除mailbox 剩余的event
                        group.getMaiBox().removeAggregateAllEventCommittingContexts(group.getAggregateId());
                        Class<T> aggreClass = ReflectUtils.getClass(group.getAggregateType());
                        domainService.getAggregateSnapshoot(group.getAggregateId(), aggreClass).whenComplete((as, e) -> {
                            if (e != null) {
                                log.error(SOURCING_EVENT_FAILTURE_MESSAGE, group.getAggregateId(), group.getAggregateType(), 1, Integer.MAX_VALUE);
                                sourcingEvent(ReflectUtils.newInstance(aggreClass), 1, Integer.MAX_VALUE);
                            }
                        }).thenAccept(as -> {
                            if (as != null) {
                                sourcingEvent(as, as.getVersion() + 1, Integer.MAX_VALUE);
                            } else {
                                T newAggregate = ReflectUtils.newInstance(ReflectUtils.getClass(group.getAggregateType()));
                                newAggregate.setId(group.getAggregateId());
                                sourcingEvent(newAggregate, 1, Integer.MAX_VALUE);
                            }
                            aggregateGroup.forEach(context -> context.getFuture().completeExceptionally(result.getThrowable()));
                        }).whenComplete((v, e) -> {
                            if (e != null) {
                                log.error("aggregate id : {}, aggregate type : {} , start event sourcing failutre", group.getAggregateId(), group.getAggregateType(), e);
                            }
                        });
                    } finally {
                        lock.unlock();
                    }
                }
            });
        }).whenComplete((v, e) -> {
            if (e != null) {
                log.error("aggregate store failture ", e);
            }
        });

    }

    private void sourcingEvent(Aggregate aggregate, int startVersion, int endVersion) {
        for (;;) {
            Class<? extends Aggregate> aggreClass = aggregate.getClass();
            try {
                log.info(SOURCING_EVENT_MESSAGE, aggregate.getId(), aggregate.getClass().getTypeName(), startVersion, endVersion);
                boolean success = eventStore.load(aggregate.getId(), aggreClass, startVersion, endVersion).thenApply(events -> {
                    events.forEach(es -> aggregate.replayEvents(es));
                    aggregateCache.updateAggregateCache(aggregate.getId(), aggregate);
                    log.info(SOURCING_EVENT_SUCCESS_MESSAGE, aggregate.getId(), aggreClass.getTypeName(), startVersion, endVersion);
                    return true;
                }).whenComplete((v, e) -> {
                    if (e != null) {
                        log.error(SOURCING_EVENT_FAILTURE_MESSAGE, aggregate.getId(), aggreClass.getTypeName(), 1, Integer.MAX_VALUE, e);
                        ThreadUtils.sleep(1000);
                    }
                }).join();
                if (success) {
                    break;
                }
            } catch (Exception e) {
                log.error(SOURCING_EVENT_FAILTURE_MESSAGE, aggregate.getId(), aggreClass.getTypeName(), 1, Integer.MAX_VALUE, e);
                ThreadUtils.sleep(1000);
            }

        }
    }

    public IEventStore getEventStore() {
        return eventStore;
    }

    public IAggregateCache getAggregateCache() {
        return aggregateCache;
    }

}
