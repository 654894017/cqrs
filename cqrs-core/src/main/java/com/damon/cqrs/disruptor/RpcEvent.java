package com.damon.cqrs.disruptor;

import com.damon.cqrs.domain.Command;

import java.util.concurrent.CompletableFuture;
import java.util.function.Function;

class RpcEvent {
    private Command request;
    private CompletableFuture responseFuture;
    private Function function;

    public Command getRequest() {
        return request;
    }

    public void setRequest(Command request) {
        this.request = request;
    }

    public Function getFunction() {
        return function;
    }

    public void setFunction(Function function) {
        this.function = function;
    }

    public CompletableFuture getResponseFuture() {
        return responseFuture;
    }

    public void setResponseFuture(CompletableFuture responseFuture) {
        this.responseFuture = responseFuture;
    }
}
