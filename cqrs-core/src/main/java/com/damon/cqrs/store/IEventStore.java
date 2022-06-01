package com.damon.cqrs.store;

import com.damon.cqrs.domain.AggregateRoot;
import com.damon.cqrs.domain.Event;
import com.damon.cqrs.event.AggregateEventAppendResult;
import com.damon.cqrs.event.DomainEventStream;
import com.damon.cqrs.event.EventSendingContext;

import java.util.List;
import java.util.Map;
import java.util.concurrent.CompletableFuture;

public interface IEventStore {

    CompletableFuture<AggregateEventAppendResult> store(List<DomainEventStream> streams);

    CompletableFuture<List<List<Event>>> load(long aggregateId, Class<? extends AggregateRoot> aggregateClass, int startVersion, int endVersion, Map<String, Object> shardingParams);

    CompletableFuture<List<EventSendingContext>> queryWaitingSendEvents(String dataSourceName, String tableName, long offsetId);
}
