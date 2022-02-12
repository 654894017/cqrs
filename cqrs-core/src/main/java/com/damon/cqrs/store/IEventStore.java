package com.damon.cqrs.store;

import com.damon.cqrs.event.AggregateEventAppendResult;
import com.damon.cqrs.DomainEventGroupKey;
import com.damon.cqrs.event.DomainEventStream;
import com.damon.cqrs.event.EventSendingContext;
import com.damon.cqrs.domain.Aggregate;
import com.damon.cqrs.domain.Event;

import java.util.List;
import java.util.Map;
import java.util.concurrent.CompletableFuture;

public interface IEventStore {

    CompletableFuture<List<AggregateEventAppendResult>> store(Map<DomainEventGroupKey, List<DomainEventStream>> map);

    CompletableFuture<List<List<Event>>> load(long aggregateId, Class<? extends Aggregate> aggregateClass, int startVersion, int endVersion);

    String getDuplicatedId(String throwable);

    CompletableFuture<List<EventSendingContext>> queryWaitingSendEvents(long offsetId);
}
