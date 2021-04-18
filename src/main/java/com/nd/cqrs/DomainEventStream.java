package com.nd.cqrs;

import java.util.List;
import java.util.concurrent.CompletableFuture;

import com.nd.cqrs.domain.Aggregate;
import com.nd.cqrs.domain.Event;

import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class DomainEventStream {
    private long commandId;
    private int version;
    private List<Event> events;
    private AggregateGroup  group;
    private CompletableFuture<Boolean> future;
    private Aggregate aggregateSnapshoot;
    
}
