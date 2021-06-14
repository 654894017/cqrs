package com.damon.cqrs;

import com.damon.cqrs.domain.Aggregate;

public interface IAggregateSnapshootService {

    void saveAggregategetSnapshoot(Aggregate aggregate);

}
