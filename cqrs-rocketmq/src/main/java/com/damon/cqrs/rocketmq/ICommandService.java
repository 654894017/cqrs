package com.damon.cqrs.rocketmq;

import java.util.concurrent.CompletableFuture;

import com.damon.cqrs.domain.Command;


public interface ICommandService {

    CompletableFuture<CommandResult> executeAsync(Command command, CommandReturnType returnType);
    
//    CommandResult execute(Command command, CommandReturnType returnType);
    
}
