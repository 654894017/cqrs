package com.damon.cqrs.sample.train.command;

import com.damon.cqrs.domain.Command;
import com.damon.cqrs.sample.train.aggregate.value_object.enum_type.SEAT_TYPE;

public class TicketProtectCancelCommand extends Command {
    private Integer startStationNumber;
    private Integer endStationNumber;
    private Boolean strict;
    private SEAT_TYPE seatType;

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

    public Boolean getStrict() {
        return strict;
    }

    public void setStrict(Boolean strict) {
        this.strict = strict;
    }

    public SEAT_TYPE getSeatType() {
        return seatType;
    }

    public void setSeatType(SEAT_TYPE seatType) {
        this.seatType = seatType;
    }
}
