package com.damon.cqrs.event;

import com.damon.cqrs.*;
import com.damon.cqrs.domain.Aggregate;
import com.damon.cqrs.store.IEventStore;
import com.damon.cqrs.utils.AggregateLockUtils;
import com.damon.cqrs.utils.ReflectUtils;
import com.damon.cqrs.utils.ThreadUtils;
import lombok.extern.slf4j.Slf4j;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.locks.ReentrantLock;
import java.util.stream.Collectors;

/**
 * 领域事件提交服务
 *
 * @author xianping_lu
 */
@Slf4j
public class EventCommittingService {

    private final List<EventCommittingMailBox> eventCommittingMailBoxs;

    private final ExecutorService service;

    private final IEventStore eventStore;

    private final int mailboxNumber;

    private final IAggregateSnapshootService aggregateSnapshootService;

    private final IAggregateCache aggregateCache;

    /**
     * @param eventStore
     * @param aggregateSnapshootService
     * @param aggregateCache
     * @param mailBoxNumber
     * @param batchSize                 批量批量提交的大小，如果event store是机械硬盘可以加大此大小。
     */
    public EventCommittingService(IEventStore eventStore, IAggregateSnapshootService aggregateSnapshootService, IAggregateCache aggregateCache, int mailBoxNumber, int batchSize) {
        this.eventCommittingMailBoxs = new ArrayList<EventCommittingMailBox>(mailBoxNumber);
        this.service = Executors.newFixedThreadPool(mailBoxNumber);
        this.mailboxNumber = mailBoxNumber;
        this.eventStore = eventStore;
        this.aggregateSnapshootService = aggregateSnapshootService;
        this.aggregateCache = aggregateCache;
        for (int number = 0; number < mailBoxNumber; number++) {
            eventCommittingMailBoxs.add(new EventCommittingMailBox(service, contexts -> batchStoreEvent(contexts), number, batchSize));
        }
    }

    /**
     * 提交聚合事件
     *
     * @param context
     */
    public void commitDomainEventAsync(EventCommittingContext context) {
        int index = (int) (Math.abs(context.getAggregateId()) % mailboxNumber);
        EventCommittingMailBox maxibox = eventCommittingMailBoxs.get(index);
        maxibox.enqueue(context);
    }

    /**
     * 批量保存聚合事件
     *
     * @param <T>
     * @param contexts
     */
    private <T extends Aggregate> void batchStoreEvent(List<EventCommittingContext> contexts) {

        List<DomainEventStream> eventStream = contexts.stream().map(context -> {
            DomainEventGroupKey group = DomainEventGroupKey.builder()
                    .aggregateId(context.getAggregateId())
                    .aggregateType(context.getAggregateTypeName())
                    .eventCommittingMailBox(context.getMailBox())
                    .build();
            return DomainEventStream.builder()
                    .future(context.getFuture())
                    .commandId(context.getCommandId())
                    .group(group)
                    .events(context.getEvents())
                    .version(context.getVersion())
                    .build();
        }).collect(Collectors.toList());

        Map<DomainEventGroupKey, List<DomainEventStream>> map = eventStream.stream().collect(Collectors.groupingBy(DomainEventStream::getGroup));
        eventStore.store(map).thenAccept(results -> {
            results.forEach(result -> {
                DomainEventGroupKey groupKey = result.getGroupKey();
                List<DomainEventStream> domainEventStreams = map.get(groupKey);
                if (EventAppendStatus.Success.equals(result.getEventAppendStatus())) {
                    domainEventStreams.forEach(context -> context.getFuture().complete(true));
                } else {
                    AbstractDomainService<T> domainService = DomainServiceContext.get(groupKey.getAggregateType());
                    // 当聚合事件保存冲突时，同时也需要锁住领域服务不能让新的Command进入领域服务，不然聚合回溯的聚合实体是不正确的，由业务调用方重新发起请求
                    ReentrantLock lock = AggregateLockUtils.getLock(groupKey.getAggregateId());
                    lock.lock();
                    // 清除mailbox 剩余的event
                    groupKey.getEventCommittingMailBox().removeAggregateAllEventCommittingContexts(groupKey.getAggregateId()).forEach((key, context) ->
                            context.getFuture().completeExceptionally(result.getThrowable())
                    );
                    domainEventStreams.forEach(context -> context.getFuture().completeExceptionally(result.getThrowable()));
                    for (; ; ) {
                        try {
                            Class<T> aggregateClass = ReflectUtils.getClass(groupKey.getAggregateType());
                            boolean success = domainService.getAggregateSnapshoot(groupKey.getAggregateId(), aggregateClass).thenCompose(snapshoot -> {
                                if (snapshoot != null) {
                                    return sourcingEvent(snapshoot, snapshoot.getVersion() + 1, Integer.MAX_VALUE);
                                } else {
                                    T newAggregate = ReflectUtils.newInstance(ReflectUtils.getClass(groupKey.getAggregateType()));
                                    newAggregate.setId(groupKey.getAggregateId());
                                    return sourcingEvent(newAggregate, 1, Integer.MAX_VALUE);
                                }
                            }).exceptionally(e -> {
                                log.error(
                                        "aggregate id: {} , type: {} , event sourcing failed. ",
                                        groupKey.getAggregateId(),
                                        groupKey.getAggregateType(),
                                        e
                                );
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

    /**
     * 回溯事件
     *
     * @param aggregate
     * @param startVersion
     * @param endVersion
     * @return
     */
    private CompletableFuture<Boolean> sourcingEvent(Aggregate aggregate, int startVersion, int endVersion) {
        log.info(
                "aggregate id: {} , type: {} , start event sourcing. start version : {}, end version : {}.",
                aggregate.getId(),
                aggregate.getClass().getTypeName(),
                startVersion,
                endVersion
        );
        return eventStore.load(aggregate.getId(), aggregate.getClass(), startVersion, endVersion).thenApply(events -> {
            events.forEach(es -> aggregate.replayEvents(es));
            aggregateCache.update(aggregate.getId(), aggregate);
            log.info(
                    "aggregate id: {} , type: {} , event sourcing succeed. start version : {}, end version : {}.",
                    aggregate.getId(),
                    aggregate.getClass().getTypeName(),
                    startVersion,
                    endVersion
            );
            return true;
        }).whenComplete((v, e) -> {
            if (e != null) {
                log.error(
                        "aggregate id: {} , type: {} , event sourcing failed. start version : {}, end version : {}.",
                        aggregate.getId(),
                        aggregate.getClass().getTypeName(),
                        startVersion,
                        endVersion,
                        e
                );
            }
        });
    }

    public IEventStore getEventStore() {
        return eventStore;
    }

    public IAggregateCache getAggregateCache() {
        return aggregateCache;
    }

    public IAggregateSnapshootService getAggregateSnapshootService() {
        return aggregateSnapshootService;
    }
}
