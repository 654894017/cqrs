package com.damon.cqrs.sample.workflow3.workflow.cmd;

import com.damon.cqrs.domain.Command;

public class ProcessStartCmd extends Command {
    private String processXml;

    public ProcessStartCmd(Long commandId, Long aggregateId) {
        super(commandId, aggregateId);
    }

    public String getProcessXml() {
        return processXml;
    }

    public void setProcessXml(String processXml) {
        this.processXml = processXml;
    }
}
