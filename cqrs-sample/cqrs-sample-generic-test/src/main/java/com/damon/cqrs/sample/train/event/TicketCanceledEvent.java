package com.damon.cqrs.sample.train.event;

import com.damon.cqrs.domain.Event;

public class TicketCanceledEvent extends Event {
    private Long userId;
    private Integer startStationNumber;
    private Integer endStationNumber;

    public TicketCanceledEvent() {

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
