package com.damon.cqrs.sample.workflow.operator;

import com.damon.cqrs.sample.workflow.PeContext;
import com.damon.cqrs.sample.workflow.PeNode;
import com.damon.cqrs.sample.workflow.ProcessEngine;

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
     * @param node
     * @param peContext
     */
    void doTask(ProcessEngine processEngine, PeNode node, PeContext peContext);
}