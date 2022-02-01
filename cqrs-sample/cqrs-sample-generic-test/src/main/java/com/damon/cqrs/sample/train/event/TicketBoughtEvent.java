package com.damon.cqrs.sample.train.event;

import com.damon.cqrs.domain.Event;
import com.damon.cqrs.sample.train.aggregate.value_object.SEAT_PROTECT_TYPE;

public class TicketBoughtEvent extends Event {
    private Long userId;
    private Integer startStationNumber;
    private Integer endStationNumber;
    private Integer seatIndex;
    private SEAT_PROTECT_TYPE seatProtectType;
    /**
     * 用来记录站点到站点间不是严格保护map的key信息
     */
    private Integer s2sSeatRelaxedProtectKey;

    public TicketBoughtEvent() {

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

    public Integer getS2sSeatRelaxedProtectKey() {
        return s2sSeatRelaxedProtectKey;
    }

    public void setS2sSeatRelaxedProtectKey(Integer s2sSeatRelaxedProtectKey) {
        this.s2sSeatRelaxedProtectKey = s2sSeatRelaxedProtectKey;
    }

}
