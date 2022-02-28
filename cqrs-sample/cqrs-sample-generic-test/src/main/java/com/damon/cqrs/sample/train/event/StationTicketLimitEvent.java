package com.damon.cqrs.sample.train.event;

import com.damon.cqrs.domain.Event;
import com.damon.cqrs.sample.train.aggregate.value_object.enum_type.SEAT_TYPE;

public class StationTicketLimitEvent extends Event {

    private Integer stationNumber;
    /**
     * 站点与站点间最多可卖票数
     */
    private Integer maxCanBuyTicketCount;

    private SEAT_TYPE seatType;

    public StationTicketLimitEvent() {

    }

    public StationTicketLimitEvent(Integer stationNumber, Integer maxCanBuyTicketCount, SEAT_TYPE seatType) {
        this.stationNumber = stationNumber;
        this.maxCanBuyTicketCount = maxCanBuyTicketCount;
        this.seatType = seatType;
    }

    public Integer getMaxCanBuyTicketCount() {
        return maxCanBuyTicketCount;
    }

    public void setMaxCanBuyTicketCount(Integer maxCanBuyTicketCount) {
        this.maxCanBuyTicketCount = maxCanBuyTicketCount;
    }

    public Integer getStationNumber() {
        return stationNumber;
    }

    public void setStationNumber(Integer stationNumber) {
        this.stationNumber = stationNumber;
    }

    public SEAT_TYPE getSeatType() {
        return seatType;
    }

    public void setSeatType(SEAT_TYPE seatType) {
        this.seatType = seatType;
    }
}
