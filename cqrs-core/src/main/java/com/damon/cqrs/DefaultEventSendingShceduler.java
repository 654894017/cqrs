package com.damon.cqrs;

import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

import lombok.extern.slf4j.Slf4j;

/**
 * 
 * 
 * 
 * 
 * 
 * @author xianping_lu
 *
 */
@Slf4j
public class DefaultEventSendingShceduler implements IEventSendingShceduler {

    private ScheduledExecutorService scheduledExecutorService;

    private IEventStore eventStore;

    private ISendMessageService sendMessageService;

    public DefaultEventSendingShceduler(final IEventStore eventStore, final ISendMessageService sendMessageService, final int aggregateSnapshootProcessThreadNumber, final int delaySeconds) {
        this.scheduledExecutorService = Executors.newSingleThreadScheduledExecutor();
        this.sendMessageService = sendMessageService;
        this.eventStore = eventStore;
        scheduledExecutorService.scheduleWithFixedDelay(() -> {
            sendEvent();
        }, 5, delaySeconds, TimeUnit.SECONDS);
    }

    @Override
    public void sendEvent() {
        try {
            for (;;) {
                Long startOffsetId = eventStore.getEventOffset().join();
                CompletableFuture<List<EventSendingContext>> futrue = eventStore.queryWaitingSend(startOffsetId);
                List<EventSendingContext> contexts = futrue.join();
                if (contexts.isEmpty()) {
                    break;
                }
                long offsetId = contexts.get(contexts.size() - 1).getOffsetId();
                log.info("event start offset id : {}， end offset id : {}", startOffsetId, offsetId);
                sendMessageService.sendMessage(contexts);
                eventStore.updateEventOffset(offsetId);
                log.info("update event offset id  :  {} ", offsetId);
            }
        } catch (Throwable e) {
            log.error("event sending  failure", e);
        }
    }

}
