package com.damon.cqrs.sample.workflow3.workflow.operator;

import com.damon.cqrs.sample.workflow3.workflow.PeContext;
import com.damon.cqrs.sample.workflow3.workflow.PeNode;
import com.damon.cqrs.sample.workflow3.workflow.PeProcess;

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
    public void doTask(PeNode peNode, PeProcess process) {
        PeContext context = process.getContext();
        //price每次减100
        context.putValue("price", price -= 100);
        context.putValue("applicant", "小张");
    }
}
