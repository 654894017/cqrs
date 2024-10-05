package com.damon.cqrs.sample.workflow3.workflow.operator;

import cn.hutool.core.map.MapUtil;
import cn.hutool.core.util.StrUtil;
import com.damon.cqrs.sample.workflow3.workflow.PeContext;
import com.damon.cqrs.sample.workflow3.workflow.PeEdge;
import com.damon.cqrs.sample.workflow3.workflow.PeNode;
import com.damon.cqrs.sample.workflow3.workflow.PeProcess;
import com.damon.cqrs.sample.workflow3.workflow.utils.ScriptEngineUtils;

/**
 * 简单是非判断
 */
public class OperatorOfExclusiveGateway implements IOperator {
    @Override
    public String getType() {
        return "exclusiveGateway";
    }

    @Override
    public void doTask(PeNode peNode, PeProcess process) {
        PeContext peContext = process.getContext();
        Integer price = (Integer) peContext.getValue("price");
        for (PeEdge peEdge : peNode.getOut()) {
            String script = peEdge.getExpandInfo();
            if (StrUtil.isNotBlank(script)) {
                boolean result = ScriptEngineUtils.execute(script, MapUtil.of("price", price));
                if (result) {
                    process.route(peEdge);
                    return;
                }
            }
        }
        throw new RuntimeException("未知的分支条件，无法继续流程");
    }
}