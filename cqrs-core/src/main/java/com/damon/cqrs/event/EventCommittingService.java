package com.damon.cqrs.event;

import com.damon.cqrs.AbstractDomainService;
import com.damon.cqrs.IAggregateCache;
import com.damon.cqrs.IAggregateSnapshootService;
import com.damon.cqrs.domain.Aggregate;
import com.damon.cqrs.store.IEventStore;
import com.damon.cqrs.utils.AggregateLockUtils;
import com.damon.cqrs.utils.NamedThreadFactory;
import com.damon.cqrs.utils.ReflectUtils;
import com.damon.cqrs.utils.ThreadUtils;
import lombok.extern.slf4j.Slf4j;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentHashMap;
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

    private final ExecutorService eventCommittingservice;

    private final ExecutorService aggregateRecoverService;

    private final IEventStore eventStore;

    private final int mailboxNumber;

    private final IAggregateSnapshootService aggregateSnapshootService;

    private final IAggregateCache aggregateCache;

    /**
     *
     * @param eventStore
     * @param aggregateSnapshootService
     * @param aggregateCache
     * @param mailBoxNumber
     * @param batchSize 批量批量提交的大小，如果event store是机械硬盘可以加大此大小。
     * @param recoverThreadNumber
     */
    public EventCommittingService(IEventStore eventStore,
                                  IAggregateSnapshootService aggregateSnapshootService,
                                  IAggregateCache aggregateCache,
                                  int mailBoxNumber,
                                  int batchSize,
                                  int recoverThreadNumber
    ) {
        this.eventCommittingMailBoxs = new ArrayList<EventCommittingMailBox>(mailBoxNumber);
        this.eventCommittingservice = Executors.newFixedThreadPool(mailBoxNumber, new NamedThreadFactory("event-committing-pool"));
        this.aggregateRecoverService = Executors.newFixedThreadPool(recoverThreadNumber, new NamedThreadFactory("aggregate-recover-pool"));
        this.mailboxNumber = mailBoxNumber;
        this.eventStore = eventStore;
        this.aggregateSnapshootService = aggregateSnapshootService;
        this.aggregateCache = aggregateCache;
        for (int number = 0; number < mailBoxNumber; number++) {
            eventCommittingMailBoxs.add(new EventCommittingMailBox(eventCommittingservice, contexts -> batchStoreEvent(contexts), number, batchSize));
        }
    }

    /**
     * 提交聚合事件
     *
     * @param context
     */
    public void commitDomainEventAsync(EventCommittingContext context) {
        EventCommittingMailBox maxibox = getEventCommittingMailBox(context.getAggregateId());
        maxibox.enqueue(context);
    }

    private EventCommittingMailBox getEventCommittingMailBox(Long aggregateId) {
        int index = (int) (Math.abs(aggregateId) % mailboxNumber);
        return eventCommittingMailBoxs.get(index);
    }

    /**
     * 批量保存聚合事件
     *
     * @param contexts
     */
    private <T extends Aggregate> void batchStoreEvent(List<EventCommittingContext> contexts) {

        List<DomainEventStream> eventStream = contexts.stream().map(context -> {
            return DomainEventStream.builder()
                    .future(context.getFuture())
                    .commandId(context.getCommandId())
                    .events(context.getEvents())
                    .version(context.getVersion())
                    .aggregateId(context.getAggregateId())
                    .aggregateType(context.getAggregateTypeName())
                    .build();
        }).collect(Collectors.toList());

        eventStore.store(eventStream).thenAccept(results -> {
            // 1.正常请求
            results.getSucceedResults().forEach(result -> result.getFuture().complete(true));
            ConcurrentHashMap<Long, Throwable> exceptionMap = new ConcurrentHashMap<>();
            // 2.重复的聚合command
            results.getDulicateCommandResults().forEach(result -> {
                Long aggregateId = result.getAggreateId();
                removeAggregateEvent(aggregateId, result.getThrowable());
                CompletableFuture.runAsync(() -> {
                    recoverAggregate(aggregateId, result.getAggregateType());
                    exceptionMap.put(aggregateId, result.getThrowable());
                }, aggregateRecoverService).join();
            });
            // 3.冲突的聚合event
            results.getDuplicateEventResults().forEach(result -> {
                Long aggregateId = result.getAggreateId();
                removeAggregateEvent(aggregateId, result.getThrowable());
                CompletableFuture.runAsync(() -> {
                    recoverAggregate(aggregateId, result.getAggregateType());
                    exceptionMap.put(aggregateId, result.getThrowable());
                }, aggregateRecoverService).join();
            });
            // 4.异常的聚合
            results.getExceptionResults().forEach(result -> {
                Long aggregateId = result.getAggreateId();
                removeAggregateEvent(aggregateId, result.getThrowable());
                CompletableFuture.runAsync(() -> {
                    recoverAggregate(aggregateId, result.getAggregateType());
                    exceptionMap.put(aggregateId, result.getThrowable());
                }, aggregateRecoverService).join();
            });
            // 5.通知异常处理
            eventStream.stream().filter(
                    domainEventStream -> exceptionMap.get(domainEventStream.getAggregateId()) != null
            ).forEach(stream ->
                    stream.getFuture().completeExceptionally(exceptionMap.get(stream.getAggregateId()))
            );
        });
    }

    private void removeAggregateEvent(Long aggregateId, Throwable e) {
        EventCommittingMailBox mailbox = getEventCommittingMailBox(aggregateId);
        mailbox.removeAggregateAllEventCommittingContexts(aggregateId).forEach((key, context) ->
                context.getFuture().completeExceptionally(e)
        );
    }


    public <T extends Aggregate> void recoverAggregate(Long aggregateId, String aggregateType) {
        AbstractDomainService<T> domainService = DomainServiceContext.get(aggregateType);
        ReentrantLock lock = AggregateLockUtils.getLock(aggregateId);
        lock.lock();
        for (; ; ) {
            try {
                Class<T> aggregateClass = ReflectUtils.getClass(aggregateType);
                boolean success = domainService.getAggregateSnapshoot(aggregateId, aggregateClass).thenCompose(snapshoot -> {
                    if (snapshoot != null) {
                        return sourcingEvent(snapshoot, snapshoot.getVersion() + 1, Integer.MAX_VALUE);
                    } else {
                        T newAggregate = ReflectUtils.newInstance(ReflectUtils.getClass(aggregateType));
                        newAggregate.setId(aggregateId);
                        return sourcingEvent(newAggregate, 1, Integer.MAX_VALUE);
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
