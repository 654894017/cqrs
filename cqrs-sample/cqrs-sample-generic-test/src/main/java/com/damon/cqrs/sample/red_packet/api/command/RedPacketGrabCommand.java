package com.damon.cqrs.sample.red_packet.api.command;

import com.damon.cqrs.domain.Command;

public class RedPacketGrabCommand extends Command {

    /**
     *
     */
    private static final long serialVersionUID = -3309773599641095159L;
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
