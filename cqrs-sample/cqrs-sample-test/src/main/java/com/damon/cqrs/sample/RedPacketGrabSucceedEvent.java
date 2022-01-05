package com.damon.cqrs.sample;

import com.damon.cqrs.domain.Aggregate;
import com.damon.cqrs.domain.Event;
import lombok.Data;

import java.util.HashMap;
import java.util.Map;
import java.util.Queue;
import java.util.concurrent.ArrayBlockingQueue;

@Data
public class RedPacketGrabSucceedEvent extends Event {

    public RedPacketGrabSucceedEvent(){

    }

    public RedPacketGrabSucceedEvent(Long money, Long userId) {
        this.money = money;
        this.userId = userId;
    }

    private Long money;

    private Long userId;

    private Long redPacketId;

}
