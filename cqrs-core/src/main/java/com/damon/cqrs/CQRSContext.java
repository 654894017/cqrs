package com.damon.cqrs;

import com.damon.cqrs.domain.AggregateRoot;

import java.util.HashMap;
import java.util.Map;

@SuppressWarnings("unchecked")
public class CQRSContext {

    public static Map<String, CommandHandler<?>> map = new HashMap<>();

    public static synchronized <T extends AggregateRoot> void add(String aggregateType, CommandHandler<T> service) {
        map.put(aggregateType, service);
    }

    public static <T extends AggregateRoot> CommandHandler<T> get(String aggregateType) {
        return (CommandHandler<T>) map.get(aggregateType);
    }
}
