package com.damon.cqrs.sample.train.event;

import com.damon.cqrs.domain.Event;

public class TicketProtectCanceledEvent extends Event {
    private Integer startStationNumber;
    private Integer endStationNumber;

    public TicketProtectCanceledEvent() {
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
