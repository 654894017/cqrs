package com.damon.cqrs.sample.train.command;

import com.damon.cqrs.domain.Command;

public class TicketProtectCommand extends Command {
    private Integer startStationNumber;
    private Integer endStationNumber;
    /**
     * 站点与站点间保留票数（最少可以卖多少张票）
     */
    private Integer minCanBuyTicketCount;
    /**
     * 站点与站点间最多可卖票数
     */
    private Integer maxCanBuyTicketCount;

    private Boolean strict;


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

    public Integer getMinCanBuyTicketCount() {
        return minCanBuyTicketCount;
    }

    public void setMinCanBuyTicketCount(Integer minCanBuyTicketCount) {
        this.minCanBuyTicketCount = minCanBuyTicketCount;
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
