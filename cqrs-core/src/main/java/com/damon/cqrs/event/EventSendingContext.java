package com.damon.cqrs.event;

import com.damon.cqrs.domain.Event;
import lombok.Builder;
import lombok.Data;

import java.util.List;

@Builder
@Data
public class EventSendingContext {
    private long offsetId;
    private List<Event> events;
    private Long aggregateId;
    private String aggregateType;
}