package com.damon.cqrs.sample.metting.api.command;

import com.damon.cqrs.domain.Command;

public class MettingGetCommand extends Command {

    public MettingGetCommand(Long commandId, Long aggregateId) {
        super(commandId, aggregateId);
    }
}
