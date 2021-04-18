package com.damon.cqrs;

import java.util.List;
import java.util.concurrent.CompletableFuture;

import com.damon.cqrs.domain.Event;

import lombok.Builder;
import lombok.Data;

@Builder
@Data
public class EventSendingContext {

    private List<Event> events;
    private CompletableFuture<Boolean> future;
    private long aggregateId;
    private String aggregateType;

}