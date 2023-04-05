package com.damon.cqrs.sample.workflow.domain;

import com.damon.cqrs.domain.Command;

public class WorkflowInstanceCreateCommand extends Command {

    public WorkflowInstanceCreateCommand(Long commandId, Long aggregateId) {
        super(commandId, aggregateId);
    }
}
