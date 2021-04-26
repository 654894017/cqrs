package com.domain.cqrs.domain;


import static com.google.common.base.Preconditions.checkNotNull;

import java.io.Serializable;

import lombok.Data;

@Data
public abstract class Command implements Serializable{

    /**
     * 
     */
    private static final long serialVersionUID = -2869549269787386287L;

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
