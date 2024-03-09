package com.damon.cqrs.sample.workflow.operator;

import com.damon.cqrs.sample.workflow.*;

import javax.script.ScriptEngine;
import javax.script.ScriptEngineManager;
import javax.script.ScriptException;

/**
 * 简单是非判断
 */
public class OperatorOfSimpleGateway implements IOperator {
    @Override
    public String getType() {
        return "simpleGateway";
    }

    @Override
    public void doTask(ProcessEngine processEngine, PeNode node, PeContext peContext) {
        ScriptEngineManager manager = new ScriptEngineManager();
        ScriptEngine engine = manager.getEngineByName("JavaScript");
        engine.put("approvalResult", peContext.getValue("approvalResult"));

        String expression = XmlUtil.childTextByName(node.xmlNode, "expr");
        String trueOutGoingEdgeID = XmlUtil.childTextByName(node.xmlNode, "trueOutGoing");

        PeEdge outPeEdge = null;
        try {
            outPeEdge = (Boolean) engine.eval(expression) ? node.outWithID(trueOutGoingEdgeID) : node.outWithOutID(trueOutGoingEdgeID);
        } catch (ScriptException e) {
            e.printStackTrace();
        }

        processEngine.nodeFinished(outPeEdge);
    }
}