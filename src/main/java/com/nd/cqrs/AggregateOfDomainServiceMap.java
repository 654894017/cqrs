package com.nd.cqrs;

import java.util.HashMap;
import java.util.Map;

import com.nd.cqrs.domain.Aggregate;
import com.nd.cqrs.domain.DomainService;

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
