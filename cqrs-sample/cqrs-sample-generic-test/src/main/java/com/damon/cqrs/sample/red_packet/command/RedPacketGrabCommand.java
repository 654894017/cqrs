package com.damon.cqrs.sample.red_packet.command;

import com.damon.cqrs.domain.Command;

public class RedPacketGrabCommand extends Command {

    private Long userId;

    public RedPacketGrabCommand(Long commandId, Long redPacketId) {
        super(commandId, redPacketId);
    }

    public Long getUserId() {
        return userId;
    }

    public void setUserId(Long userId) {
        this.userId = userId;
    }
}
