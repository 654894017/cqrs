package com.damon.cqrs.domain;

import java.io.Serializable;
import java.util.Map;

import static com.google.common.base.Preconditions.checkNotNull;

public abstract class Command implements Serializable {

    /**
     *
     */
    private static final long serialVersionUID = -2869549269787386287L;

    private Long commandId;

    private Long aggregateId;

    private Map<String, Object> shardingParams;


    public Command(Long commandId, Long aggregateId, Map<String, Object> shardingParams) {
        checkNotNull(commandId);
        checkNotNull(aggregateId);
        this.commandId = commandId;
        this.aggregateId = aggregateId;
        this.shardingParams = shardingParams;
    }

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

    public Map<String, Object> getShardingParams() {
        return shardingParams;
    }

    public void setShardingParams(Map<String, Object> shardingParams) {
        this.shardingParams = shardingParams;
    }

    @Override
    public int hashCode() {
        return Long.hashCode(aggregateId);
    }

}
