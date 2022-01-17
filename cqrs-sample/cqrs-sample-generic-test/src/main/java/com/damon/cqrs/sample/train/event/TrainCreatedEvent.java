package com.damon.cqrs.sample.train.event;


import com.damon.cqrs.domain.Event;

import java.util.concurrent.ConcurrentSkipListMap;

public class TrainCreatedEvent extends Event {

    private ConcurrentSkipListMap<Integer, Integer> s2sSeatCount;

    public TrainCreatedEvent() {

    }

    public ConcurrentSkipListMap<Integer, Integer> getS2sSeatCount() {
        return s2sSeatCount;
    }

    public void setS2sSeatCount(ConcurrentSkipListMap<Integer, Integer> s2sSeatCount) {
        this.s2sSeatCount = s2sSeatCount;
    }
}

