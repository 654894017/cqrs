package com.damon.cqrs.event;

import com.damon.cqrs.store.IEventOffset;
import com.damon.cqrs.store.IEventStore;
import com.damon.cqrs.utils.NamedThreadFactory;
import com.damon.cqrs.utils.ThreadUtils;
import lombok.extern.slf4j.Slf4j;

import java.util.List;
import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.stream.Collectors;

/**
 * 事件发送调度器
 *
 * @author xianping_lu
 */
@Slf4j
public class DefaultEventSendingShceduler implements IEventSendingShceduler {

    private final ScheduledExecutorService scheduledExecutorService;

    private final IEventOffset eventOffset;

    private final IEventStore eventStore;

    private final EventSendingService eventSendingService;

    public DefaultEventSendingShceduler(
            final IEventStore eventStore,
            final IEventOffset eventOffset,
            final EventSendingService eventSendingService,
            final int delaySeconds) {
        this.scheduledExecutorService = Executors.newSingleThreadScheduledExecutor(new NamedThreadFactory("event-scheduler-pool"));
        this.eventOffset = eventOffset;
        this.eventStore = eventStore;
        this.eventSendingService = eventSendingService;
        scheduledExecutorService.scheduleWithFixedDelay(() -> {
            try {
                sendEvent();
            } catch (Throwable e) {
                log.error("event sending failed", e);
            }
        }, 5, delaySeconds, TimeUnit.SECONDS);
    }

    @Override
    public void sendEvent() {
        for (; ; ) {
            List<Map<String, Object>> rows = eventOffset.queryEventOffset().join();
            AtomicInteger count = new AtomicInteger(0);
            for (Map<String, Object> map : rows) {
                Long eventOffsetId = (Long) map.get("event_offset_id");
                String dataSourceName = (String) map.get("data_source_name");
                String tableName = (String) map.get("table_name");
                Long id = (Long) map.get("id");
                CompletableFuture<List<EventSendingContext>> futrue = eventStore.queryWaitingSendEvents(
                        dataSourceName, tableName, eventOffsetId
                );
                List<EventSendingContext> contexts = futrue.join();
                if (contexts.isEmpty()) {
                    count.addAndGet(1);
                    continue;
                }
                long offsetId = contexts.get(contexts.size() - 1).getOffsetId();
                log.info("event start offset id : {}， end offset id : {}, dataSourceName : {}, tableName: {}, id :{}",
                        eventOffsetId, offsetId, dataSourceName, tableName, id);
                List<CompletableFuture<Boolean>> futures = contexts.stream().map(context -> {
                    CompletableFuture<Boolean> future = new CompletableFuture<>();
                    context.setFuture(future);
                    eventSendingService.sendDomainEventAsync(context);
                    return future;
                }).collect(Collectors.toList());
                try {
                    CompletableFuture.allOf(futures.toArray(new CompletableFuture[0])).thenAccept(v -> {
                        eventOffset.updateEventOffset(dataSourceName, offsetId, id);
                        log.info("update event offset id :  {}, dataSourceName : {}, tableName: {}, id :{} ", offsetId, dataSourceName, tableName, id);
                    }).join();
                } catch (Throwable e) {
                    log.error("event sending failed", e);
                    ThreadUtils.sleep(2000);
                }
            }
            //所有表都检查一遍，如果无数据需要发送，则跳出循环。
            if (count.intValue() == rows.size()) {
                log.info("event sending succeed");
                return;
            }
        }

    }

}
