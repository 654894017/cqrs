package com.damon.cqrs;

import com.damon.cqrs.domain.Aggregate;

import java.util.HashMap;
import java.util.Map;

@SuppressWarnings("unchecked")
public class DomainServiceContext {

    public static Map<String, AbstractDomainService<?>> map = new HashMap<>();

    public static synchronized <T extends Aggregate> void add(String aggregateType, AbstractDomainService<T> service) {
        map.put(aggregateType, service);
    }

    public static <T extends Aggregate> AbstractDomainService<T> get(String aggregateType) {
        return (AbstractDomainService<T>) map.get(aggregateType);
    }
}
