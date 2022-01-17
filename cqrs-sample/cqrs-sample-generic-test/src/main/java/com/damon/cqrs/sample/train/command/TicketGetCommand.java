package com.damon.cqrs.sample.train.command;

import com.damon.cqrs.domain.Command;

public class TicketGetCommand extends Command {
    /**
     * @param commandId
     * @param aggregateId
     */
    public TicketGetCommand(long commandId, long aggregateId) {
        super(commandId, aggregateId);
    }

}
