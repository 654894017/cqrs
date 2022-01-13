package com.damon.cqrs.sample.weixin.command;

import com.damon.cqrs.domain.Command;

public class RedPacketCreateCommand extends Command {

    private Long money;

    private int number;
    /**
     * 红包类型 1 平均分配  2 随机分配
     */
    private RedPacketTypeEnum type;
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

    public Long getMoney() {
        return money;
    }

    public void setMoney(Long money) {
        this.money = money;
    }

    public int getNumber() {
        return number;
    }

    public void setNumber(int number) {
        this.number = number;
    }

    public RedPacketTypeEnum getType() {
        return type;
    }

    public void setType(RedPacketTypeEnum type) {
        this.type = type;
    }

    public Long getSponsorId() {
        return sponsorId;
    }

    public void setSponsorId(Long sponsorId) {
        this.sponsorId = sponsorId;
    }
}
