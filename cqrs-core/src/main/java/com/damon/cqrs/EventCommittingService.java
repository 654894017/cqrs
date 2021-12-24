package com.damon.cqrs;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.locks.ReentrantLock;
import java.util.stream.Collectors;

import com.damon.cqrs.domain.Aggregate;
import com.damon.cqrs.utils.ReflectUtils;
import com.damon.cqrs.utils.ThreadUtils;

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
    private final String SOURCING_EVENT_SUCCEED_MESSAGE = "aggregate id: {} , type: {} , event sourcing succeed. start version : {}, end version : {}.";
    private final String SOURCING_EVENT_FAILED_MESSAGE = "aggregate id: {} , type: {} , event sourcing failed. start version : {}, end version : {}.";

    /**
     * 
     * @param eventStore
     * @param aggregateSnapshootService
     * @param aggregateCache
     * @param mailBoxNumber
     * @param batchSize                 批量批量提交的大小，如果event store是机械硬盘可以加大此大小。
     */
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
     * @param context
     */
    public void commitDomainEventAsync(EventCommittingContext context) {
        int index = (int) (Math.abs(context.getAggregateId()) % mailboxNumber);
        EventCommittingMailBox maxibox = mailBoxs.get(index);
        maxibox.enqueue(context);
    }

    /**
     * 批量保存聚合事件
     * @param <T>
     * @param contexts
     */
    private <T extends Aggregate> void batchStorageEvent(List<EventCommittingContext> contexts) {
        List<DomainEventStream> eventStream = contexts.stream().map(context -> {
            AggregateGroup group = AggregateGroup.builder().aggregateId(context.getAggregateId())//
                    .aggregateType(context.getAggregateTypeName()).eventCommittingMailBox(context.getMailBox()).build();
            return DomainEventStream.builder().snapshoot(context.getSnapshoot()).future(context.getFuture()).commandId(context.getCommandId()).group(group)
                    .events(context.getEvents()).version(context.getVersion()).build();
        }).collect(Collectors.toList());
        Map<AggregateGroup, List<DomainEventStream>> map = eventStream.stream().collect(Collectors.groupingBy(stream -> stream.getGroup()));
        eventStore.store(map).thenAccept(results -> {
            results.forEach(result -> {
                AggregateGroup group = result.getGroup();
                List<DomainEventStream> aggregateGroup = map.get(group);
                if (EventAppendStatus.Success.equals(result.getEventAppendStatus())) {
                    DomainEventStream stream = aggregateGroup.get(aggregateGroup.size() - 1);
                    Aggregate snapshoot = stream.getSnapshoot();
                    if(snapshoot != null) {
                        aggregateSnapshootService.saveAggregategetSnapshoot(snapshoot);
                    }
                    aggregateGroup.forEach(context -> context.getFuture().complete(true));
                } else {
                    AbstractDomainService<T> domainService = DomainServiceContext.get(group.getAggregateType());
                    // 当聚合事件保存冲突时，同时也需要锁住领域服务不能让新的Command进入领域服务，不然聚合回溯的聚合实体是不正确的，由业务调用方重新发起请求
                    ReentrantLock lock = AggregateLock.getLock(group.getAggregateId());
                    lock.lock();
                    // 清除mailbox 剩余的event
                    group.getEventCommittingMailBox().removeAggregateAllEventCommittingContexts(group.getAggregateId()).forEach((key, context) -> 
                        context.getFuture().completeExceptionally(result.getThrowable())
                    );
                    aggregateGroup.forEach(context -> context.getFuture().completeExceptionally(result.getThrowable()));
                    for (;;) {
                        try {
                            Class<T> aggregateClass = ReflectUtils.getClass(group.getAggregateType());
                            boolean success = domainService.getAggregateSnapshoot(group.getAggregateId(), aggregateClass).thenCompose(snapshoot -> {
                                if (snapshoot != null) {
                                    return sourcingEvent(snapshoot, snapshoot.getVersion() + 1, Integer.MAX_VALUE);
                                } else {
                                    T newAggregate = ReflectUtils.newInstance(ReflectUtils.getClass(group.getAggregateType()));
                                    newAggregate.setId(group.getAggregateId());
                                    return sourcingEvent(newAggregate, 1, Integer.MAX_VALUE);
                                }
                            }).exceptionally(e -> {
                                log.error("aggregate id: {} , type: {} , event sourcing failed. ", group.getAggregateId(), group.getAggregateType(), e);
                                ThreadUtils.sleep(2000);
                                return false;
                            }).join();
                            if (success) {
                                break;
                            }
                        } finally {
                            lock.unlock();
                        }
                    }
                }
            });
        });
    }

    private CompletableFuture<Boolean> sourcingEvent(Aggregate aggregate, int startVersion, int endVersion) {
        log.info(SOURCING_EVENT_MESSAGE, aggregate.getId(), aggregate.getClass().getTypeName(), startVersion, endVersion);
        return eventStore.load(aggregate.getId(), aggregate.getClass(), startVersion, endVersion).thenApply(events -> {
            events.forEach(es -> aggregate.replayEvents(es));
            aggregateCache.update(aggregate.getId(), aggregate);
            log.info(SOURCING_EVENT_SUCCEED_MESSAGE, aggregate.getId(), aggregate.getClass().getTypeName(), startVersion, endVersion);
            return true;
        }).whenComplete((v, e) -> {
            if (e != null) {
                log.error(SOURCING_EVENT_FAILED_MESSAGE, aggregate.getId(), aggregate.getClass().getTypeName(), startVersion, endVersion, e);
            }
        });
    }

    public IEventStore getEventStore() {
        return eventStore;
    }

    public IAggregateCache getAggregateCache() {
        return aggregateCache;
    }

}
