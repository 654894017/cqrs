package com.damon.cqrs.sample.workflow;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

public class PeContext {
    private Map<String, Object> info = new ConcurrentHashMap<>();

    public Object getValue(String key) {
        return info.get(key);
    }

    public void putValue(String key, Object value) {
        info.put(key, value);
    }
}