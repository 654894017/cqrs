package com.damon.cqrs.rocketmq.core;

import com.damon.cqrs.domain.Command;

public class ACKCommand extends Command {

    /**
     *
     */
    private static final long serialVersionUID = -1054063014509404578L;
    private CommandACKStatus status;


    public ACKCommand(long commandId, long aggregateId) {
        super(commandId, aggregateId);
    }

    public CommandACKStatus getStatus() {
        return status;
    }

    public void setStatus(CommandACKStatus status) {
        this.status = status;
    }

}
