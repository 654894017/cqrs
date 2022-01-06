package com.damon.cqrs.rocketmq.core;

import com.alipay.remoting.AsyncContext;
import com.alipay.remoting.BizContext;
import com.alipay.remoting.rpc.protocol.AsyncUserProcessor;
import com.damon.cqrs.domain.Command;

public class CommandRequestProcessorAsync extends AsyncUserProcessor<Command> {

    @Override
    public void handleRequest(BizContext bizCtx, AsyncContext asyncCtx, Command command) {
        CommandResult result = new CommandResult();
        if (command != null) {
            System.out.println("recive request" + command);
            result.setAggregateId(command.getAggregateId());
            result.setCommandId(command.getCommandId());
            asyncCtx.sendResponse(result);
        }
    }

    @Override
    public String interest() {
        return TestCommand.class.getName();
    }
}