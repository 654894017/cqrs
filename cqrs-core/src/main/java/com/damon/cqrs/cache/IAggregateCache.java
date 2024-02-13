package com.damon.cqrs.cache;

import com.damon.cqrs.domain.AggregateRoot;

public interface IAggregateCache {

    void update(long id, AggregateRoot aggregate);

    <T extends AggregateRoot> T get(long id);

}
