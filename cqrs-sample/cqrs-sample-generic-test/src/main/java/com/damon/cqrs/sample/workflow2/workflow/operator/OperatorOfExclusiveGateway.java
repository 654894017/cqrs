package com.damon.cqrs.sample.workflow2.workflow.operator;

import cn.hutool.core.map.MapUtil;
import cn.hutool.core.util.StrUtil;
import com.damon.cqrs.sample.workflow2.workflow.PeContext;
import com.damon.cqrs.sample.workflow2.workflow.PeEdge;
import com.damon.cqrs.sample.workflow2.workflow.PeNode;
import com.damon.cqrs.sample.workflow2.workflow.ProcessEngine;
import com.damon.cqrs.sample.workflow2.workflow.utils.ScriptEngineUtils;

/**
 * 简单是非判断
 */
public class OperatorOfExclusiveGateway implements IOperator {
    @Override
    public String getType() {
        return "exclusiveGateway";
    }

    @Override
    public void doTask(ProcessEngine processEngine, PeNode peNode, PeContext peContext) {
        Integer price = (Integer) peContext.getValue("price");
        for (PeEdge peEdge : peNode.getOut()) {
            String script = peEdge.getExpandInfo();
            if (StrUtil.isNotBlank(script)) {
                boolean result = ScriptEngineUtils.execute(script, MapUtil.of("price", price));
                if (result) {
                    processEngine.nodeFinished(peEdge);
                    return;
                }
            }
        }
        throw new RuntimeException("未知的分支条件，无法继续流程");
    }
}