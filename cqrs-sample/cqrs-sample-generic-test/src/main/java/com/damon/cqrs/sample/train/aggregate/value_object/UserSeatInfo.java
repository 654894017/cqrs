package com.damon.cqrs.sample.train.aggregate.value_object;

/**
 * 用户车次坐席信息
 */
public class UserSeatInfo {

    private Integer startStationNumber;

    private Integer endStationNumber;

    private Integer seatIndex;

    private SEAT_PROTECT_TYPE type;

    private Integer s2sSeatRelaxedProtectKey;

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

    public SEAT_PROTECT_TYPE getType() {
        return type;
    }

    public void setType(SEAT_PROTECT_TYPE type) {
        this.type = type;
    }

    public Integer getS2sSeatRelaxedProtectKey() {
        return s2sSeatRelaxedProtectKey;
    }

    public void setS2sSeatRelaxedProtectKey(Integer s2sSeatRelaxedProtectKey) {
        this.s2sSeatRelaxedProtectKey = s2sSeatRelaxedProtectKey;
    }
}