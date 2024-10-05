package com.damon.cqrs.sample.workflow2.workflow.operator;

import com.damon.cqrs.sample.workflow2.workflow.PeContext;
import com.damon.cqrs.sample.workflow2.workflow.PeNode;
import com.damon.cqrs.sample.workflow2.workflow.ProcessEngine;

/**
 * 提交申请单
 */
public class OperatorOfApprovalApply implements IOperator {

    public static int price = 500;

    @Override
    public String getType() {
        return "approvalApply";
    }

    @Override
    public void doTask(ProcessEngine processEngine, PeNode peNode, PeContext peContext) {
        //price每次减100
        peContext.putValue("price", price -= 100);
        peContext.putValue("applicant", "小张");

        processEngine.nodeFinished(peNode.onlyOneOut());
    }
}
