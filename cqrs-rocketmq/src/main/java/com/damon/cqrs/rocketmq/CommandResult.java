package com.damon.cqrs.rocketmq;

import java.io.Serializable;

public class CommandResult implements Serializable{
    
    /**
     * 
     */
    private static final long serialVersionUID = -1928143149039315342L;

    private CommandStatus status;
    
    private Long commandId;
    
    private Long aggregateId;
    
    private String resultType;
    
    private String result;

    public CommandStatus getStatus() {
        return status;
    }

    public void setStatus(CommandStatus status) {
        this.status = status;
    }

    public Long getCommandId() {
        return commandId;
    }

    public void setCommandId(Long commandId) {
        this.commandId = commandId;
    }

    public Long getAggregateId() {
        return aggregateId;
    }

    public void setAggregateId(Long aggregateId) {
        this.aggregateId = aggregateId;
    }

    public String getResultType() {
        return resultType;
    }

    public void setResultType(String resultType) {
        this.resultType = resultType;
    }

    public String getResult() {
        return result;
    }

    public void setResult(String result) {
        this.result = result;
    }

    @Override
    public String toString() {
        return "CommandResult [status=" + status + ", commandId=" + commandId + ", aggregateId=" + aggregateId
            + ", resultType=" + resultType + ", result=" + result + "]";
    }

    
    
}
