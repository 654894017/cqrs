package com.damon.cqrs;

import java.util.List;
import java.util.Map;
import java.util.concurrent.CompletableFuture;

import com.damon.cqrs.domain.Aggregate;
import com.damon.cqrs.domain.Event;

public interface IEventStore {

    CompletableFuture<List<AggregateEventAppendResult>> store(Map<AggregateGroup, List<DomainEventStream>> map);

    CompletableFuture<List<List<Event>>> load(long aggregateId, Class<? extends Aggregate> aggregateClass, int startVersion, int endVersion);

    String getDuplicatedId(String throwable);

    CompletableFuture<List<EventSendingContext>> queryWaitingSendEvents(long offsetId);

    CompletableFuture<Boolean> updateEventOffset(long offsetId);

    CompletableFuture<Long> getEventOffset();

}
