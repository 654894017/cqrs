package com.damon.cqrs.event;

import com.damon.cqrs.domain.Event;
import lombok.Builder;
import lombok.Data;

import java.util.List;
import java.util.Map;

@Data
@Builder
public class DomainEventStream {
    private Long aggregateId;
    private String aggregateType;
    private long commandId;
    private int version;
    private List<Event> events;
    private Map<String, Object> shardingParams;
}
