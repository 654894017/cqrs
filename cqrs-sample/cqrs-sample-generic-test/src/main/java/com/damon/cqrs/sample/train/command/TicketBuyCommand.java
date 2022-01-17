package com.damon.cqrs.sample.train.command;

import com.damon.cqrs.domain.Command;

public class TicketBuyCommand extends Command {
    private Long userId;
    private Integer startStationNumber;
    private Integer endStationNumber;

    /**
     * @param commandId
     * @param aggregateId
     */
    public TicketBuyCommand(long commandId, long aggregateId) {
        super(commandId, aggregateId);
    }

    public Long getUserId() {
        return userId;
    }

    public void setUserId(Long userId) {
        this.userId = userId;
    }

    public Integer getStartStationNumber() {
        return startStationNumber;
    }

    public void setStartStationNumber(Integer startStationNumber) {
        this.startStationNumber = startStationNumber;
    }

    public Integer getEndStationNumber() {
        return endStationNumber;
    }

    public void setEndStationNumber(Integer endStationNumber) {
        this.endStationNumber = endStationNumber;
    }
}
