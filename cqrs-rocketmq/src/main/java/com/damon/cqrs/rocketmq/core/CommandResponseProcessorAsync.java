package com.damon.cqrs.rocketmq.core;

import com.alipay.remoting.AsyncContext;
import com.alipay.remoting.BizContext;
import com.alipay.remoting.rpc.protocol.AsyncUserProcessor;

import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentHashMap;

public class CommandResponseProcessorAsync extends AsyncUserProcessor<CommandResult> {

    private ConcurrentHashMap<Long, CompletableFuture<CommandResult>> commandCallbackMap = new ConcurrentHashMap<>();

    public void addCommandResultCallback(Long commandId, CompletableFuture<CommandResult> future) {
        commandCallbackMap.put(commandId, future);
    }

    @Override
    public void handleRequest(BizContext bizCtx, AsyncContext asyncCtx, CommandResult commandResult) {

        CompletableFuture<CommandResult> resultFuture = commandCallbackMap.get(commandResult.getCommandId());

        if (resultFuture != null) {
            resultFuture.complete(commandResult);
            ACKCommand ack = new ACKCommand(commandResult.getCommandId(), commandResult.getAggregateId());
            ack.setStatus(CommandACKStatus.SUCCEED);
            asyncCtx.sendResponse(ack);
        }
    }

    @Override
    public String interest() {
        return CommandResult.class.getName();
    }
}