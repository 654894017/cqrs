package com.damon.cqrs.sample.train.command;

import com.damon.cqrs.domain.Command;

public class TicketProtectCommand extends Command {
    private Integer startStationNumber;
    private Integer endStationNumber;
    /**
     * 站点与站点间保留票数（最少可以卖多少张票）
     */
    private Integer count;
    /**
     * 站点与站点间最多可卖票数
     */
    private Integer maxCanBuyTicketCount;


    /**
     * @param commandId
     * @param aggregateId
     */
    public TicketProtectCommand(long commandId, long aggregateId) {
        super(commandId, aggregateId);
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

    public Integer getCount() {
        return count;
    }

    public void setCount(Integer count) {
        this.count = count;
    }

    public Integer getMaxCanBuyTicketCount() {
        return maxCanBuyTicketCount;
    }

    public void setMaxCanBuyTicketCount(Integer maxCanBuyTicketCount) {
        this.maxCanBuyTicketCount = maxCanBuyTicketCount;
    }
}
