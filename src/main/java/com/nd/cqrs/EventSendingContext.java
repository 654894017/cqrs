package com.nd.cqrs;

import java.util.List;
import java.util.concurrent.CompletableFuture;

import com.nd.cqrs.domain.Event;

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