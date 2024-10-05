package com.damon.cqrs.sample.workflow3.workflow.cmd;

import com.damon.cqrs.domain.Command;


public class ProcessApprovalCmd extends Command {
    private String nodeId;

    public ProcessApprovalCmd(Long commandId, Long aggregateId) {
        super(commandId, aggregateId);
    }

    public String getNodeId() {
        return nodeId;
    }

    public void setNodeId(String nodeId) {
        this.nodeId = nodeId;
    }
}
