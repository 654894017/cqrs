package com.damon.cqrs;

import com.domain.cqrs.domain.Aggregate;

public interface IAggregateSnapshootService {

    void addAggregategetSnapshoot(Aggregate aggregate);

}
