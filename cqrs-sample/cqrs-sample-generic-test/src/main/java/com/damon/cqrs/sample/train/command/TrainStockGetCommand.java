package com.damon.cqrs.sample.train.command;

import com.damon.cqrs.domain.Command;

public class TrainStockGetCommand extends Command {
    /**
     * @param commandId
     * @param aggregateId
     */
    public TrainStockGetCommand(long commandId, long aggregateId) {
        super(commandId, aggregateId);
    }

}
