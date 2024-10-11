package com.damon.cqrs.disruptor2;

import java.util.concurrent.CompletableFuture;

class RpcEvent {
    private String request;
    // 用于异步响应
    private CompletableFuture<String> responseFuture;

    public String getRequest() {
        return request;
    }

    public void setRequest(String request) {
        this.request = request;
    }

    public CompletableFuture<String> getResponseFuture() {
        return responseFuture;
    }

    public void setResponseFuture(CompletableFuture<String> responseFuture) {
        this.responseFuture = responseFuture;
    }
}
