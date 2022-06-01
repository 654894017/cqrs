package com.damon.cqrs.event;

import com.damon.cqrs.domain.Event;
import lombok.Builder;
import lombok.Data;

import java.util.List;
import java.util.Map;
import java.util.concurrent.CompletableFuture;

@Data
@Builder
public class DomainEventStream {
    private long commandId;
    private int version;
    private List<Event> events;
    private CompletableFuture<Boolean> future;
    private String aggregateType;
    private Long aggregateId;
    private Map<String, Object> shardingParams;
}
