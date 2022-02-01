package com.damon.cqrs.sample.train.event;

import com.damon.cqrs.domain.Event;
import com.damon.cqrs.sample.train.aggregate.value_object.SEAT_PROTECT_TYPE;

public class TicketCanceledEvent extends Event {
    private Long userId;
    private Integer startStationNumber;
    private Integer endStationNumber;
    private Integer seatIndex;
    private SEAT_PROTECT_TYPE seatProtectType;

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

    public Integer getSeatIndex() {
        return seatIndex;
    }

    public void setSeatIndex(Integer seatIndex) {
        this.seatIndex = seatIndex;
    }

    public SEAT_PROTECT_TYPE getSeatProtectType() {
        return seatProtectType;
    }

    public void setSeatProtectType(SEAT_PROTECT_TYPE seatProtectType) {
        this.seatProtectType = seatProtectType;
    }
}
