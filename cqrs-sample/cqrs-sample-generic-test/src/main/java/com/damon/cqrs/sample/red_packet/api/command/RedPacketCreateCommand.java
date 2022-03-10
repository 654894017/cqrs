package com.damon.cqrs.sample.red_packet.api.command;

import com.damon.cqrs.domain.Command;

public class RedPacketCreateCommand extends Command {

    private Double money;

    private int number;
    /**
     * 红包发起人id
     */
    private Long sponsorId;

    /**
     * @param commandId
     * @param aggregateId
     */
    public RedPacketCreateCommand(long commandId, long aggregateId) {
        super(commandId, aggregateId);
    }

    public Double getMoney() {
        return money;
    }

    public void setMoney(Double money) {
        this.money = money;
    }

    public int getNumber() {
        return number;
    }

    public void setNumber(int number) {
        this.number = number;
    }

    public Long getSponsorId() {
        return sponsorId;
    }

    public void setSponsorId(Long sponsorId) {
        this.sponsorId = sponsorId;
    }
}
