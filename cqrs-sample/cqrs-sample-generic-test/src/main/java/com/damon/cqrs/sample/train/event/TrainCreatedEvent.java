package com.damon.cqrs.sample.train.event;


import com.damon.cqrs.domain.Event;
import com.damon.cqrs.sample.train.domain.TrainStock;

import java.util.concurrent.ConcurrentSkipListMap;

public class TrainCreatedEvent extends Event {

    private ConcurrentSkipListMap<Integer, TrainStock.StationSeatInfo> s2sSeatCount;

    public TrainCreatedEvent() {

    }

    public ConcurrentSkipListMap<Integer, TrainStock.StationSeatInfo> getS2sSeatCount() {
        return s2sSeatCount;
    }

    public void setS2sSeatCount(ConcurrentSkipListMap<Integer, TrainStock.StationSeatInfo> s2sSeatCount) {
        this.s2sSeatCount = s2sSeatCount;
    }
}

