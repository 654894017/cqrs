package com.damon.cqrs.snapshot;

import com.damon.cqrs.domain.AggregateRoot;

public interface IAggregateSnapshootService {

    void saveAggregateSnapshot(AggregateRoot aggregate);

}
