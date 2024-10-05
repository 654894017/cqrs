package com.damon.cqrs.sample.workflow2.workflow.operator;

import com.damon.cqrs.sample.workflow2.workflow.PeContext;
import com.damon.cqrs.sample.workflow2.workflow.PeNode;
import com.damon.cqrs.sample.workflow2.workflow.ProcessEngine;

public interface IOperator {
    /**
     * 引擎可以据此来找到本算子
     *
     * @return
     */
    String getType();

    /**
     * 引擎调度本算子
     *
     * @param processEngine
     * @param peNode
     * @param peContext
     */
    void doTask(ProcessEngine processEngine, PeNode peNode, PeContext peContext);
}