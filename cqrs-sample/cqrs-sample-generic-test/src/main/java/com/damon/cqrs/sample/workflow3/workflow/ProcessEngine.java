package com.damon.cqrs.sample.workflow3.workflow;

import com.damon.cqrs.sample.workflow3.workflow.operator.IOperator;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

public class ProcessEngine {
    //存储算子
    private static Map<String, IOperator> type2Operator = new ConcurrentHashMap<>();

    public static void doTask(PeNode peNode, PeProcess process) {
        type2Operator.get(peNode.getType()).doTask(peNode, process);
    }

    //算子注册到引擎中，便于引擎调用之
    public static void registNodeProcessor(IOperator operator) {
        type2Operator.put(operator.getType(), operator);
    }

}