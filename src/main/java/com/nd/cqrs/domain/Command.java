package com.nd.cqrs.domain;

import static com.google.common.base.Preconditions.checkNotNull;

import lombok.Data;

@Data
public abstract class Command {

    private long commandId;
    
    private long aggregateId;

    /**
     * @param commandId
     * @param aggregateId
     */
    
    public Command(long commandId, long aggregateId) {
        checkNotNull(commandId);
        checkNotNull(aggregateId);
        this.commandId = commandId;
        this.aggregateId = aggregateId;
    }
    
    

    
}
