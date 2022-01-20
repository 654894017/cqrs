package com.damon.cqrs.sample.train.event;

import com.damon.cqrs.domain.Event;

public class TicketProtectSucceedEvent extends Event {
    private Integer startStationNumber;
    private Integer endStationNumber;
    private Integer count;
    /**
     * 保留的座位票索引，需要用 BitSet.valueOf(arr)反解析回来
     */
    private long[] protectSeatIndex;

    public TicketProtectSucceedEvent() {
    }

    public Integer getCount() {
        return count;
    }

    public void setCount(Integer count) {
        this.count = count;
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

    public long[] getProtectSeatIndex() {
        return protectSeatIndex;
    }

    public void setProtectSeatIndex(long[] protectSeatIndex) {
        this.protectSeatIndex = protectSeatIndex;
    }
}
