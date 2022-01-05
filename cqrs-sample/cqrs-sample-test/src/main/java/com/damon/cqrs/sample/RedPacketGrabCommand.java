package com.damon.cqrs.sample;

import com.damon.cqrs.domain.Command;
import com.damon.cqrs.domain.Event;
import lombok.Data;

public class RedPacketGrabCommand extends Command {

    public RedPacketGrabCommand(Long commandId, Long redPacketId){
        super(commandId,redPacketId);
    }

    private Long userId;

    public Long getUserId() {
        return userId;
    }

    public void setUserId(Long userId) {
        this.userId = userId;
    }
}
