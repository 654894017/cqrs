package com.damon.cqrs.sample.workflow.domain;

import com.damon.cqrs.domain.AggregateRoot;
import lombok.Data;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * xianping_lu
 *
 *
 */
public class Workflow2 extends AggregateRoot {

    private String workflowName;
    private String status;

    private Map<String, String> processingNode = new HashMap<>(2);

    public void start(){

    }

    public void end(){
        this.status = "end";
    }

    /**
     * 停止流程节点
     */
    public void stop() {
        this.status = "stop";
    }

    /**
     * 跳转任意流程
     */
    public void jump(String targetId) {

    }

    /**
     * 返回上一流程节点
     */
    public void goback() {

    }



}
