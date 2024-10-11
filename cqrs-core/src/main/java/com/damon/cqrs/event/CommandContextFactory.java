package com.damon.cqrs.event;

import com.damon.cqrs.command.CommandContext;
import com.lmax.disruptor.EventFactory;

import java.util.concurrent.CompletableFuture;

public class CommandContextFactory<R> implements EventFactory<CommandContext> {
    @Override
    public CommandContext newInstance() {
        return new CommandContext(new CompletableFuture<R>());
    }
}
