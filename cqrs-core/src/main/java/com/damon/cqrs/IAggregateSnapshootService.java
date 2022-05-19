package com.damon.cqrs;

import com.damon.cqrs.domain.AggregateRoot;

public interface IAggregateSnapshootService {

    void saveAggregategetSnapshot(AggregateRoot aggregate);

}
