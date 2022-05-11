package com.damon.cqrs.sample.red_packet.api.event;

import com.damon.cqrs.domain.Event;
import lombok.Data;

import java.util.Stack;

@Data
public class RedPacketCreatedEvent extends Event {

    /**
     *
     */
    private static final long serialVersionUID = 2969013356235525800L;

    private Stack<Double> redpacketStack;

    private Long sponsorId;

    private Double money;

    private int size;

    public RedPacketCreatedEvent() {
        super();
    }

    public RedPacketCreatedEvent(Stack<Double> redpacketStack) {
        super();
        this.redpacketStack = redpacketStack;
    }


}
