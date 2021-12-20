package com.damon.cqrs;

import java.util.List;
import java.util.concurrent.CompletableFuture;

import com.damon.cqrs.domain.Aggregate;
import com.damon.cqrs.domain.Event;

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
    private Aggregate snapshoot;
    
}
