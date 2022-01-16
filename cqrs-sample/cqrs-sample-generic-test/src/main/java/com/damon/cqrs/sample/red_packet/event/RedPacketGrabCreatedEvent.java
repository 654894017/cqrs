package com.damon.cqrs.sample.red_packet.event;

import com.damon.cqrs.domain.Event;
import com.damon.cqrs.sample.red_packet.command.RedPacketTypeEnum;
import lombok.Data;

import java.util.Stack;

@Data
public class RedPacketGrabCreatedEvent extends Event {

    /**
     *
     */
    private static final long serialVersionUID = 1L;

    private Stack<Long> redpacketStack;

    private RedPacketTypeEnum type;

    private Long sponsorId;

    public RedPacketGrabCreatedEvent() {
        super();
    }

    public RedPacketGrabCreatedEvent(Stack<Long> redpacketStack) {
        super();
        this.redpacketStack = redpacketStack;
    }


}
