package com.damon.cqrs;

import com.damon.cqrs.command.ICommandService;
import com.damon.cqrs.domain.AggregateRoot;

import java.util.HashMap;
import java.util.Map;

@SuppressWarnings("unchecked")
public class CqrsApplicationContext {

    private static Map<String, ICommandService<?>> map = new HashMap<>();

    public static synchronized <T extends AggregateRoot> void add(String aggregateType, ICommandService<T> service) {
        map.put(aggregateType, service);
    }

    public static <T extends AggregateRoot> ICommandService<T> get(String aggregateType) {
        return (ICommandService<T>) map.get(aggregateType);
    }
}
