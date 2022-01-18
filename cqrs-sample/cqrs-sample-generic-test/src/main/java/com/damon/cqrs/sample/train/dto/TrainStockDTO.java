package com.damon.cqrs.sample.train.dto;

import java.util.Map;
import java.util.concurrent.ConcurrentSkipListMap;

public class TrainStockDTO {

    private ConcurrentSkipListMap<Integer, Integer> s2sSeatCount;

    private Long id;

    public ConcurrentSkipListMap<Integer, Integer> getS2sSeatCount() {
        return s2sSeatCount;
    }

    public void setS2sSeatCount(ConcurrentSkipListMap<Integer, Integer> s2sSeatCount) {
        this.s2sSeatCount = s2sSeatCount;
    }

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }
}
