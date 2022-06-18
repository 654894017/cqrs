package com.damon.cqrs;

import com.damon.cqrs.event.EventCommittingService;
import com.damon.cqrs.store.IEventStore;
import lombok.Builder;
import lombok.Data;

@Builder
@Data
public class CQRSConfig {

    private IAggregateCache aggregateCache;

    private IEventStore eventStore;

    private IAggregateSnapshootService aggregateSnapshootService;

    private IBeanCopy beanCopy;

    private EventCommittingService eventCommittingService;

}
