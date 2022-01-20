package com.damon.cqrs.sample.train.event;


import com.damon.cqrs.domain.Event;

import java.util.List;

public class TrainCreatedEvent extends Event {

    private Integer seatCount;

    private List<Integer> station2StationList;

    public TrainCreatedEvent() {

    }

    public Integer getSeatCount() {
        return seatCount;
    }

    public void setSeatCount(Integer seatCount) {
        this.seatCount = seatCount;
    }

    public void setSation2StaionList(List<Integer> station2StationList) {
        this.station2StationList = station2StationList;
    }

    public List<Integer> getStation2StationList() {
        return station2StationList;
    }

    public void setStation2StationList(List<Integer> station2StationList) {
        this.station2StationList = station2StationList;
    }
}

