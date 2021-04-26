package com.damon.cqrs;

import java.util.List;
import java.util.concurrent.CompletableFuture;

import com.domain.cqrs.domain.Aggregate;
import com.domain.cqrs.domain.Event;

import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class EventCommittingContext {

    private long commandId;

    private List<Event> events;

    private CompletableFuture<Boolean> future;

    private int version;

    private EventCommittingMailBox mailBox;

    private Aggregate aggregateSnapshoot;

    

}
