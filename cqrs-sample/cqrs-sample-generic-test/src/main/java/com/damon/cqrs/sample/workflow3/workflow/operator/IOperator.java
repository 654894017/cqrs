package com.damon.cqrs.sample.workflow3.workflow.operator;

import com.damon.cqrs.sample.workflow3.workflow.PeNode;
import com.damon.cqrs.sample.workflow3.workflow.PeProcess;

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
    void doTask(PeNode peNode, PeProcess process);
}