package com.damon.cqrs;

import java.util.List;
import java.util.concurrent.CompletableFuture;

import com.domain.cqrs.domain.Event;

import lombok.Builder;
import lombok.Data;

@Builder
@Data
public class EventSendingContext {
    private long offsetId;
    private List<Event> events;
    private long aggregateId;
    private String aggregateType;
    private CompletableFuture<Boolean> future;

}