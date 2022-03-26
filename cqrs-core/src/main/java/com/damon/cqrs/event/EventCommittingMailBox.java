package com.damon.cqrs.event;

import com.damon.cqrs.exception.DuplicateEventStreamException;
import lombok.extern.slf4j.Slf4j;

import java.time.ZonedDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.function.Consumer;

import static java.time.ZonedDateTime.now;

/**
 * @author xianpinglu
 */
@Slf4j
public class EventCommittingMailBox {

    private final int mailboxNumber;
    private final int batchCommitSize;
    private final Consumer<List<EventCommittingContext>> handler;
    private final ExecutorService service;
    private final AtomicBoolean onRunning = new AtomicBoolean(false);
    private final ConcurrentLinkedQueue<EventCommittingContext> eventQueue = new ConcurrentLinkedQueue<>();
    private final ConcurrentHashMap<Long, ConcurrentHashMap<String, EventCommittingContext>> aggregateDictDict = new ConcurrentHashMap<>();
    private ZonedDateTime lastActiveTime;

    public EventCommittingMailBox(ExecutorService service, Consumer<List<EventCommittingContext>> handler, final int mailboxNumber, final int batchCommitSize) {
        this.mailboxNumber = mailboxNumber;
        this.batchCommitSize = batchCommitSize;
        this.handler = handler;
        this.lastActiveTime = now();
        this.service = service;
    }

    public void enqueue(EventCommittingContext context) {
        ConcurrentHashMap<String, EventCommittingContext> aggregateDict = aggregateDictDict.computeIfAbsent(
                context.getAggregateId(),
                key -> new ConcurrentHashMap<>()
        );
        String eventId = context.getAggregateId() + ":" + context.getVersion();
        if (aggregateDict.putIfAbsent(eventId, context) == null) {
            context.setMailBox(this);
            eventQueue.add(context);
            lastActiveTime = now();
            tryRun();
        } else {
            String message = String.format("aggregate id : %s , aggregate type : %s  event stream already exist in the EventCommittingMailBox, eventId: %s",
                    context.getAggregateId(),
                    context.getAggregateTypeName(),
                    eventId
            );
            throw new DuplicateEventStreamException(message);
        }

    }

    private void tryRun() {
        if (onRunning.compareAndSet(false, true)) {
            service.submit(() -> {
                try {
                    process();
                }catch (Throwable e){
                    log.error("event stream sumbit failed.", e);
                }
            });
        }
    }

    private void setAsNotRunning() {
        onRunning.compareAndSet(true, false);
    }

    private boolean noUnHandledMessage() {
        return eventQueue.isEmpty();
    }

    private void completeRun() {

        lastActiveTime = now();
        if (log.isDebugEnabled()) {
            log.debug("{} complete run, mailboxNumber: {}", this.getClass(), mailboxNumber);
        }

        setAsNotRunning();

        if (!noUnHandledMessage()) {
            tryRun();
            return;
        }
    }

    public ZonedDateTime getLastActiveTime() {
        return lastActiveTime;
    }

    /**
     * 移除聚合所有待提交的事件 (聚合更新冲突时使用)
     *
     * @param aggregateId
     * @return
     */
    public ConcurrentHashMap<String, EventCommittingContext> removeAggregateAllEventCommittingContexts(long aggregateId) {
        return aggregateDictDict.remove(aggregateId);
    }

    private void process() {
        lastActiveTime = now();
        List<EventCommittingContext> events = new ArrayList<>();
        while (events.size() < batchCommitSize) {
            EventCommittingContext context = eventQueue.poll();
            if (context != null) {
                ConcurrentHashMap<String, EventCommittingContext> eventMap = aggregateDictDict.getOrDefault(
                        context.getAggregateId(),
                        null
                );
                String eventId = context.getAggregateId() + ":" + context.getVersion();
                if (eventMap != null && eventMap.remove(eventId) != null) {
                    events.add(context);
                }
            } else {
                break;
            }
        }

        int size = events.size();
        if (size == 0) {
            completeRun();
            return;
        }
//        log.warn("{} batch process events , mailboxNumber : {}, batch size : {}",
//                this.getClass(), mailboxNumber,
//                events.size()
//        );
        if (size == batchCommitSize) {
            log.warn("{} batch process events , mailboxNumber : {}, batch size : {}",
                    this.getClass(), mailboxNumber,
                    events.size()
            );
        }

        try {
            handler.accept(events);
        } finally {
            completeRun();
        }
    }

}
