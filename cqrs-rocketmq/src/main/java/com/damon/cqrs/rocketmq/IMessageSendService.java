package com.damon.cqrs.rocketmq;

import java.util.concurrent.CompletableFuture;

import com.damon.cqrs.domain.Command;

public interface IMessageSendService {

    CompletableFuture<Boolean> sendMessage(Command command);
    
}
