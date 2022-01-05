package com.damon.cqrs.domain;

import java.io.Serializable;

import static com.google.common.base.Preconditions.checkNotNull;

public abstract class Command implements Serializable {

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

    public long getCommandId() {
        return commandId;
    }

    public void setCommandId(long commandId) {
        this.commandId = commandId;
    }

    public long getAggregateId() {
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
