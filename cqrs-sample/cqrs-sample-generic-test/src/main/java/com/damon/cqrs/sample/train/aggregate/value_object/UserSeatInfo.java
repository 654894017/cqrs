package com.damon.cqrs.sample.train.aggregate.value_object;

import com.damon.cqrs.sample.train.aggregate.value_object.enum_type.SEAT_PROTECT_TYPE;
import com.damon.cqrs.sample.train.aggregate.value_object.enum_type.SEAT_TYPE;

/**
 * 用户车次坐席信息
 */
public class UserSeatInfo {

    private Integer startStationNumber;

    private Integer endStationNumber;

    private Integer seatIndex;

    private SEAT_PROTECT_TYPE seatProtectType;

    private Integer s2sSeatRelaxedProtectKey;

    private SEAT_TYPE seatType;

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

    public SEAT_TYPE getSeatType() {
        return seatType;
    }

    public void setSeatType(SEAT_TYPE seatType) {
        this.seatType = seatType;
    }
}