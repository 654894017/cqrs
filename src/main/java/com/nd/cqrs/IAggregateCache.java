package com.nd.cqrs;

import com.nd.cqrs.domain.Aggregate;

public interface IAggregateCache {

    void updateAggregateCache(long id, Aggregate aggregate);

    <T extends Aggregate> T get(long id);

}
