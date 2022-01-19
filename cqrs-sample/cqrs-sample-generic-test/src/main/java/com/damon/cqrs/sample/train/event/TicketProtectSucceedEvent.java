package com.damon.cqrs.sample.train.event;

import com.damon.cqrs.domain.Event;

public class TicketProtectSucceedEvent extends Event {
    private Integer startStationNumber;
    private Integer endStationNumber;
    private Integer count;

    public TicketProtectSucceedEvent() {
    }

    public Integer getCount() {
        return count;
    }

    public void setCount(Integer count) {
        this.count = count;
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
