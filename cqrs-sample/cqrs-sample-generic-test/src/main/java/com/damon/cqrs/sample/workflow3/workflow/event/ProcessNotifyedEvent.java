package com.damon.cqrs.sample.workflow3.workflow.event;

import com.damon.cqrs.domain.Event;
import lombok.Data;

@Data
public class ProcessNotifyedEvent extends Event {
    private String nextNodeId;
    private String curNodeId;
    private String curNodeType;

    public ProcessNotifyedEvent() {
    }

}
