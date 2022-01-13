package com.damon.cqrs.sample.weixin.command;

import com.damon.cqrs.domain.Command;

public class RedPacketGetCommand extends Command {

    /**
     * @param commandId
     * @param aggregateId
     */
    public RedPacketGetCommand(long commandId, long aggregateId) {
        super(commandId, aggregateId);
    }
}
