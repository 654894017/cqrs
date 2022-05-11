package com.damon.cqrs.domain;

import java.io.Serializable;

import static com.google.common.base.Preconditions.checkNotNull;

public abstract class Command implements Serializable {

    /**
     *
     */
    private static final long serialVersionUID = -2869549269787386287L;

    private Long commandId;

    private Long aggregateId;

    /**
     * @param commandId
     * @param aggregateId
     */

    public Command(Long commandId, Long aggregateId) {
        checkNotNull(commandId);
        checkNotNull(aggregateId);
        this.commandId = commandId;
        this.aggregateId = aggregateId;
    }

    public Long getCommandId() {
        return commandId;
    }

    public void setCommandId(long commandId) {
        this.commandId = commandId;
    }

    public Long getAggregateId() {
        return aggregateId;
    }

    public void setAggregateId(long aggregateId) {
        this.aggregateId = aggregateId;
    }

    @Override
    public int hashCode() {
        return Long.hashCode(aggregateId);
    }

}
