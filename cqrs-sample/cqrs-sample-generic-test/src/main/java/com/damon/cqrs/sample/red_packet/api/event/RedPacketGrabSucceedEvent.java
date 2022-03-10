package com.damon.cqrs.sample.red_packet.api.event;

import com.damon.cqrs.domain.Event;
import lombok.Data;

@Data
public class RedPacketGrabSucceedEvent extends Event {

    private Double money;
    private Long userId;
    private Long redPacketId;

    public RedPacketGrabSucceedEvent() {
        super();
    }

    public RedPacketGrabSucceedEvent(Double money, Long userId) {
        this.money = money;
        this.userId = userId;
    }

}
