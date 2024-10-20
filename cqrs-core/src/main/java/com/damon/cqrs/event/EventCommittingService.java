package com.damon.cqrs.event;

import com.damon.cqrs.domain.AggregateRoot;
import com.damon.cqrs.recovery.AggregateRecoveryService;
import com.damon.cqrs.store.IEventStore;
import com.damon.cqrs.utils.NamedThreadFactory;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.stream.Collectors;

/**
 * 领域事件提交服务
 *
 * @author xianping_lu
 */
public class EventCommittingService {

    private final List<EventCommittingMailBox> eventCommittingMailBoxs;

    private final ExecutorService eventCommittingService;

    private final ExecutorService aggregateRecoverService;

    private final IEventStore eventStore;

    private final int mailboxNumber;

    private final AggregateRecoveryService aggregateRecoveryService;

    /**
     * @param eventStore
     * @param mailBoxNumber            不建议设置过大的数值（会导致磁盘顺序写，变成随机写模式，性能下降）
     * @param eventBatchStoreSize      事件批量提交的大小，如果event store是机械硬盘可以加大此大小。
     * @param recoverCorePoolSize      聚合事件冲突恢复最大线程数
     * @param aggregateRecoveryService
     */
    public EventCommittingService(IEventStore eventStore,
                                  int mailBoxNumber,
                                  int eventBatchStoreSize,
                                  int recoverCorePoolSize,
                                  AggregateRecoveryService aggregateRecoveryService
    ) {
        this.eventCommittingMailBoxs = new ArrayList<>(mailBoxNumber);
        this.eventCommittingService = Executors.newFixedThreadPool(mailBoxNumber, new NamedThreadFactory("event-committing-pool"));
        this.aggregateRecoverService = Executors.newFixedThreadPool(recoverCorePoolSize, new NamedThreadFactory("aggregate-recover-pool"));
        this.mailboxNumber = mailBoxNumber;
        this.eventStore = eventStore;
        this.aggregateRecoveryService = aggregateRecoveryService;
        for (int number = 0; number < mailBoxNumber; number++) {
            eventCommittingMailBoxs.add(new EventCommittingMailBox(eventCommittingService, this::batchStoreEvent, number, eventBatchStoreSize));
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
        int hash = aggregateId.hashCode();
        if (hash < 0) {
            hash = Math.abs(hash);
        }
        int index = hash % mailboxNumber;
        return eventCommittingMailBoxs.get(index);
    }

    /**
     * 批量保存聚合事件
     *
     * @param contexts
     */
    private <T extends AggregateRoot> void batchStoreEvent(List<EventCommittingContext> contexts) {
        Map<Long, Map<String, Object>> shardingParamsMap = new HashMap<>();
        Map<Long, List<EventCommittingContext>> aggregateEventMap = new HashMap<>();
        List<DomainEventStream> eventStream = contexts.stream().map(context -> {
            shardingParamsMap.putIfAbsent(context.getAggregateId(), context.getShardingParams());
            aggregateEventMap.computeIfAbsent(context.getAggregateId(), aggregateId -> new ArrayList<>()).add(context);
            return DomainEventStream.builder().commandId(context.getCommandId()).events(context.getEvents())
                    .version(context.getVersion()).shardingParams(context.getShardingParams())
                    .aggregateId(context.getAggregateId()).aggregateType(context.getAggregateTypeName()).build();
        }).collect(Collectors.toList());
        AggregateEventAppendResult results = eventStore.store(eventStream);
        // 1.存储成功
        results.getSucceedResults().forEach(result -> {
            aggregateEventMap.get(result.getAggregateId()).forEach(context -> context.getFuture().complete(true));
        });
        // 2.重复的聚合command
        results.getDulicateCommandResults().forEach(result -> {
            asyncRecoveryAggregate(
                    shardingParamsMap, result.getAggreateId(), result.getAggregateType()
            ).thenAccept(r -> {
                aggregateEventMap.get(result.getAggreateId()).forEach(event -> event.getFuture().completeExceptionally(result.getThrowable()));
                removeAggregateEvent(result.getAggreateId(), result.getThrowable());
            }).join();
        });
        // 3.冲突的聚合event
        results.getDuplicateEventResults().forEach(result -> {
            asyncRecoveryAggregate(
                    shardingParamsMap, result.getAggreateId(), result.getAggregateType()
            ).thenAccept(r -> {
                aggregateEventMap.get(result.getAggreateId()).forEach(event -> event.getFuture().completeExceptionally(result.getThrowable()));
                removeAggregateEvent(result.getAggreateId(), result.getThrowable());
            }).join();
        });
        // 4.异常的聚合
        results.getExceptionResults().forEach(result -> {
            asyncRecoveryAggregate(
                    shardingParamsMap, result.getAggreateId(), result.getAggregateType()
            ).thenAccept(r -> {
                aggregateEventMap.get(result.getAggreateId()).forEach(event -> event.getFuture().completeExceptionally(result.getThrowable()));
                removeAggregateEvent(result.getAggreateId(), result.getThrowable());
            }).join();
        });
    }

    private CompletableFuture<Void> asyncRecoveryAggregate(Map<Long, Map<String, Object>> shardingParamsMap, Long aggreateId, String aggregateType) {
        return CompletableFuture.runAsync(() -> aggregateRecoveryService.recoverAggregate(
                aggreateId, aggregateType, shardingParamsMap.get(aggreateId)
        ), aggregateRecoverService);
    }

    private void removeAggregateEvent(Long aggregateId, Throwable e) {
        EventCommittingMailBox mailbox = getEventCommittingMailBox(aggregateId);
        mailbox.removeAggregateAllEventCommittingContexts(aggregateId)
                .forEach((key, context) -> context.getFuture().completeExceptionally(e));
    }
}
