package com.damon.cqrs.sample.workflow3.workflow.event;

import com.damon.cqrs.domain.Event;
import com.damon.cqrs.sample.workflow3.workflow.PeContext;
import lombok.Data;

@Data
public class ProcessExclusivedEvent extends Event {
    private String nextNodeId;
    private String nextNodeType;
    private String curNodeId;
    private String curNodeType;
    private PeContext context;

    public ProcessExclusivedEvent() {
    }

}
