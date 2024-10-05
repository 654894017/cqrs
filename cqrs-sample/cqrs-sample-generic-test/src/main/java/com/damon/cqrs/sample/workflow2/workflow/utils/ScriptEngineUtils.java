package com.damon.cqrs.sample.workflow2.workflow.utils;

import javax.script.ScriptEngine;
import javax.script.ScriptEngineManager;
import javax.script.ScriptException;
import java.util.Map;

public class ScriptEngineUtils {

    public static boolean execute(String script, Map<String, Object> params) {
        ScriptEngineManager manager = new ScriptEngineManager();
        ScriptEngine engine = manager.getEngineByName("JavaScript");
        for (String key : params.keySet()) {
            engine.put(key, params.get(key));
        }
        try {
            return (boolean) engine.eval(script);
        } catch (ScriptException e) {
            throw new RuntimeException(e);
        }
    }
}
