package com.damon.cqrs.sample.train.event;

import com.damon.cqrs.domain.Command;
import com.damon.cqrs.domain.Event;

public class StationTicketLimitEvent extends Event {

    public StationTicketLimitEvent(){

    }

    public StationTicketLimitEvent(Integer stationNumber, Integer maxCanBuyTicketCount) {
        this.stationNumber = stationNumber;
        this.maxCanBuyTicketCount = maxCanBuyTicketCount;
    }

    private Integer stationNumber;

    /**
     * 站点与站点间最多可卖票数
     */
    private Integer maxCanBuyTicketCount;


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
}
