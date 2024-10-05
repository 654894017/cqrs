package com.damon.cqrs.sample.workflow2.workflow.operator;

import com.damon.cqrs.sample.workflow2.workflow.PeContext;
import com.damon.cqrs.sample.workflow2.workflow.PeNode;
import com.damon.cqrs.sample.workflow2.workflow.ProcessEngine;

/**
 * 审批
 */
public class OperatorOfUserTask implements IOperator {
    @Override
    public String getType() {
        return "userTask";
    }

    @Override
    public void doTask(ProcessEngine processEngine, PeNode peNode, PeContext peContext) {
        peContext.putValue("approver", "经理");
        Integer price = (Integer) peContext.getValue("price");
        //价格<=200审批才通过，即：approvalResult=true
        boolean approvalResult = price <= 200;
        peContext.putValue("approvalResult", approvalResult);
        System.out.println("approvalResult ：" + approvalResult + "，price : " + price);
        processEngine.nodeFinished(peNode.onlyOneOut());
    }
}
