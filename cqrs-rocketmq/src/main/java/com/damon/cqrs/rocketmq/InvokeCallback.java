package com.damon.cqrs.rocketmq;

public interface InvokeCallback {
    void operationComplete(final ResponseFuture responseFuture);
}