package com.damon.cqrs.event;

import com.damon.cqrs.store.IEventOffset;
import com.damon.cqrs.store.IEventStore;
import com.damon.cqrs.utils.ThreadUtils;
import lombok.extern.slf4j.Slf4j;

import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

/**
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
            final int aggregateSnapshootProcessThreadNumber,
            final int delaySeconds) {
        this.scheduledExecutorService = Executors.newSingleThreadScheduledExecutor();
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
            Long startOffsetId = eventOffset.getEventOffset().join();
            CompletableFuture<List<EventSendingContext>> futrue = eventStore.queryWaitingSendEvents(startOffsetId);
            List<EventSendingContext> contexts = futrue.join();
            if (contexts.isEmpty()) {
                break;
            }
            long offsetId = contexts.get(contexts.size() - 1).getOffsetId();
            log.info("event start offset id : {}， end offset id : {}", startOffsetId, offsetId);
            List<CompletableFuture<Boolean>> futures = contexts.stream().map(context -> {
                CompletableFuture<Boolean> future = new CompletableFuture<>();
                context.setFuture(future);
                eventSendingService.sendDomainEventAsync(context);
                return future;
            }).collect(Collectors.toList());
            try {
                CompletableFuture.allOf(futures.toArray(new CompletableFuture[0])).thenAccept(v -> {
                    eventOffset.updateEventOffset(offsetId);
                    log.info("update event offset id  :  {} ", offsetId);
                }).join();
            } catch (Throwable e) {
                log.error("event sending failed", e);
                ThreadUtils.sleep(2000);
            }

        }

    }

}
