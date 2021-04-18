package com.nd.cqrs;

import static java.time.ZonedDateTime.now;

import java.time.ZonedDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.function.Consumer;

import lombok.extern.slf4j.Slf4j;

/**
 * 
 * 
 * 
 * 
 * @author xianping_lu
 *
 * 
 */
@Slf4j
public class EventSendingMailBox {
    private ExecutorService service;
    private ZonedDateTime lastActiveTime;
    private final int mailboxNumber;
    private AtomicBoolean onRunning = new AtomicBoolean(false);
    private ConcurrentLinkedQueue<EventSendingContext> eventQueue = new ConcurrentLinkedQueue<>();
    private final int batchCommitSize;
    private final Consumer<List<EventSendingContext>> handler;

    public EventSendingMailBox(ExecutorService service, Consumer<List<EventSendingContext>> handler, int mailboxNumber, int batchCommitSize) {
        this.mailboxNumber = mailboxNumber;
        this.batchCommitSize = batchCommitSize;
        this.handler = handler;
        this.lastActiveTime = now();
        this.service = service;
    }

    public void enqueue(EventSendingContext event) {
        eventQueue.add(event);
        lastActiveTime = now();
        tryRun();
    }

    private void tryRun() {
        if (onRunning.compareAndSet(false, true)) {
            service.submit(() -> {
                process();
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

    private void process() {
        lastActiveTime = now();
        List<EventSendingContext> events = new ArrayList<>();
        while (events.size() < batchCommitSize) {
            EventSendingContext event = eventQueue.poll();
            if (event != null) {
                events.add(event);
            } else {
                break;
            }
        }
        if (events.size() == 0) {
            completeRun();
            return;
        }
        if (log.isDebugEnabled()) {
            log.debug("{} batch process events , mailboxNumber : {}, batch size : {}", this.getClass(), mailboxNumber, events.size());
        }
        try {
            handler.accept(events);
        } finally {
            completeRun();
        }
    }

}
