package com.nd.cqrs;

import java.util.List;
import java.util.concurrent.CompletableFuture;

import com.nd.cqrs.domain.Aggregate;
import com.nd.cqrs.domain.Event;

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
