package com.damon.cqrs;

import com.damon.cqrs.domain.Aggregate;

public interface IAggregateCache {

    void update(long id, Aggregate aggregate);

    <T extends Aggregate> T get(long id);

}
