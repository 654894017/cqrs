package com.damon.cqrs;

import com.damon.cqrs.command.CommandService;
import com.damon.cqrs.domain.AggregateRoot;

import java.util.HashMap;
import java.util.Map;

@SuppressWarnings("unchecked")
public class CqrsApplicationContext {

    private static Map<String, CommandService<?>> map = new HashMap<>();

    public static synchronized <T extends AggregateRoot> void add(String aggregateType, CommandService<T> service) {
        map.put(aggregateType, service);
    }

    public static <T extends AggregateRoot> CommandService<T> get(String aggregateType) {
        return (CommandService<T>) map.get(aggregateType);
    }
}
