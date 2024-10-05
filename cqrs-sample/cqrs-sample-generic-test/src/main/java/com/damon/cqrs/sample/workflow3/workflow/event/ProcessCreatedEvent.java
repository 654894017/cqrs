package com.damon.cqrs.sample.workflow3.workflow.event;

import com.damon.cqrs.domain.Event;
import com.damon.cqrs.sample.workflow3.workflow.PeNode;
import lombok.Data;

@Data
public class ProcessCreatedEvent extends Event {
    private PeNode start;
    private String expandInfo;
    private String xml;

    public ProcessCreatedEvent() {
    }

    public ProcessCreatedEvent(Long aggregatedId, String xml) {
        super.setAggregateId(aggregatedId);
        this.xml = xml;
    }
}
