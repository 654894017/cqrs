package com.damon.cqrs.sample.train.event;

import com.damon.cqrs.domain.Event;

public class TicketProtectSucceedEvent extends Event {
    private Integer startStationNumber;
    private Integer endStationNumber;
    private Integer protectCanBuyTicketCount;
    /**
     * 保留的座位票索引，需要用 BitSet.valueOf(arr)反解析回来
     */
    private long[] protectSeatIndex;

    private Integer maxCanBuyTicketCount;

    private Boolean strict;


    public TicketProtectSucceedEvent() {
    }

    public Integer getProtectCanBuyTicketCount() {
        return protectCanBuyTicketCount;
    }

    public void setProtectCanBuyTicketCount(Integer protectCanBuyTicketCount) {
        this.protectCanBuyTicketCount = protectCanBuyTicketCount;
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

    public Integer getMaxCanBuyTicketCount() {
        return maxCanBuyTicketCount;
    }

    public void setMaxCanBuyTicketCount(Integer maxCanBuyTicketCount) {
        this.maxCanBuyTicketCount = maxCanBuyTicketCount;
    }

    public Boolean getStrict() {
        return strict;
    }

    public void setStrict(Boolean strict) {
        this.strict = strict;
    }
}
