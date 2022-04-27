package com.damon.cqrs.event;

import com.damon.cqrs.AbstractDomainService;
import com.damon.cqrs.domain.AggregateRoot;

import java.util.HashMap;
import java.util.Map;

@SuppressWarnings("unchecked")
public class CQRSContext {

    public static Map<String, AbstractDomainService<?>> map = new HashMap<>();

    public static synchronized <T extends AggregateRoot> void add(String aggregateType, AbstractDomainService<T> service) {
        map.put(aggregateType, service);
    }

    public static <T extends AggregateRoot> AbstractDomainService<T> get(String aggregateType) {
        return (AbstractDomainService<T>) map.get(aggregateType);
    }
}
