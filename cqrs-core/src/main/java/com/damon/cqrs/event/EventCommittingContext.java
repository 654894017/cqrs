package com.damon.cqrs.event;

import com.damon.cqrs.domain.AggregateRoot;
import com.damon.cqrs.domain.Event;
import lombok.Builder;
import lombok.Data;

import java.util.List;
import java.util.concurrent.CompletableFuture;

@Data
@Builder
public class EventCommittingContext {

    private long commandId;

    private List<Event> events;

    private CompletableFuture<Boolean> future;

    private Long aggregateId;

    private String aggregateTypeName;

    private int version;

    private EventCommittingMailBox mailBox;

    private AggregateRoot snapshoot;


}
