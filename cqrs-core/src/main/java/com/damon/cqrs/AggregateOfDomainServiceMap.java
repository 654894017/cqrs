package com.damon.cqrs;

import java.util.HashMap;
import java.util.Map;

import com.domain.cqrs.domain.Aggregate;

@SuppressWarnings("unchecked")
public class AggregateOfDomainServiceMap {

    public static Map<String, DomainService<?>> map = new HashMap<>();

    public static synchronized <T extends Aggregate> void add(String aggregateType, DomainService<T> service) {
        map.put(aggregateType, service);
    }

    public static <T extends Aggregate> DomainService<T> get(String aggregateType) {
        return (DomainService<T>) map.get(aggregateType);
    }
}
