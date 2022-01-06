package com.damon.cqrs.rocketmq.core;

import com.damon.cqrs.domain.Command;

import java.util.concurrent.CompletableFuture;


public interface ICommandService {

    CompletableFuture<CommandResult> executeAsync(Command command, CommandReturnType returnType);

//    CommandResult execute(Command command, CommandReturnType returnType);

}
