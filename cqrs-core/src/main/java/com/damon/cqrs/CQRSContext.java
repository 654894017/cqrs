package com.damon.cqrs;

import com.damon.cqrs.domain.AggregateRoot;

import java.util.HashMap;
import java.util.Map;

@SuppressWarnings("unchecked")
public class CQRSContext {

    public static Map<String, AbstractCommandHandler<?>> map = new HashMap<>();

    public static synchronized <T extends AggregateRoot> void add(String aggregateType, AbstractCommandHandler<T> service) {
        map.put(aggregateType, service);
    }

    public static <T extends AggregateRoot> AbstractCommandHandler<T> get(String aggregateType) {
        return (AbstractCommandHandler<T>) map.get(aggregateType);
    }
}
