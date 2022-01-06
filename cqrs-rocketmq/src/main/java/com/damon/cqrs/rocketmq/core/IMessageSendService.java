package com.damon.cqrs.rocketmq.core;

import com.damon.cqrs.domain.Command;

import java.util.concurrent.CompletableFuture;

public interface IMessageSendService {

    CompletableFuture<Boolean> sendMessage(Command command);

}
