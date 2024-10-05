package com.damon.cqrs.sample.workflow3.workflow;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

public class PeContext {
    private final String PROCESS_INSTANCE = "PROCESS_INSTANCE";
    private Map<String, Object> info = new ConcurrentHashMap<>();

    public Object getValue(String key) {
        return info.get(key);
    }

    public PeProcess getProcess() {
        return (PeProcess) info.get(PROCESS_INSTANCE);
    }

    public void setProcess(PeProcess process) {
        info.put(PROCESS_INSTANCE, process);
    }

    public void putValue(String key, Object value) {
        info.put(key, value);
    }

    public Map<String, Object> getInfo() {
        return info;
    }

    public void setInfo(Map<String, Object> info) {
        this.info = info;
    }
}