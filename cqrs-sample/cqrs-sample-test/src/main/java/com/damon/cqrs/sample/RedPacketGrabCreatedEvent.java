package com.damon.cqrs.sample;

import com.damon.cqrs.domain.Event;
import lombok.Data;

import java.util.Stack;

@Data
public class RedPacketGrabCreatedEvent extends Event {

    public RedPacketGrabCreatedEvent(Stack<Long> redpacketStack ) {
        this.redpacketStack = redpacketStack;
    }

    private Stack<Long> redpacketStack;

    private RedPacketTypeEnum type;

    private Long sponsorId;

    public Stack<Long> getRedpacketStack() {
        return redpacketStack;
    }

    public void setRedpacketStack(Stack<Long> redpacketStack) {
        this.redpacketStack = redpacketStack;
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
