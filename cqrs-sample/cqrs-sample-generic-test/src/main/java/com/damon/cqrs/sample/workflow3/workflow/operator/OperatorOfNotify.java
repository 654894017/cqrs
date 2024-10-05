package com.damon.cqrs.sample.workflow3.workflow.operator;

import com.damon.cqrs.sample.workflow3.workflow.PeContext;
import com.damon.cqrs.sample.workflow3.workflow.PeNode;
import com.damon.cqrs.sample.workflow3.workflow.PeProcess;

/**
 * 结果邮件通知
 */
public class OperatorOfNotify implements IOperator {
    @Override
    public String getType() {
        return "notify";
    }

    @Override
    public void doTask(PeNode peNode, PeProcess process) {
        PeContext peContext = process.getContext();
        System.out.println(String.format("%s 提交的申请单 %s 被 %s 审批，结果为 %s",
                peContext.getValue("applicant"),
                peContext.getValue("price"),
                peContext.getValue("approver"),
                peContext.getValue("approvalResult")));
    }
}
