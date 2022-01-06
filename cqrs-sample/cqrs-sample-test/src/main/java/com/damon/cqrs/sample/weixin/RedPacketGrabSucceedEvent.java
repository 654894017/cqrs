package com.damon.cqrs.sample.weixin;

import com.damon.cqrs.domain.Event;
import lombok.Data;

@Data
public class RedPacketGrabSucceedEvent extends Event {

    private Long money;
    private Long userId;
    private Long redPacketId;

    public RedPacketGrabSucceedEvent() {
        super();
    }

    public RedPacketGrabSucceedEvent(Long money, Long userId) {
        this.money = money;
        this.userId = userId;
    }

}
