package com.damon.cqrs.sample.train.command;

import com.damon.cqrs.domain.Command;

public class TicketProtectCancelCommand extends Command {
    private Integer startStationNumber;
    private Integer endStationNumber;


    /**
     * @param commandId
     * @param aggregateId
     */
    public TicketProtectCancelCommand(long commandId, long aggregateId) {
        super(commandId, aggregateId);
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
