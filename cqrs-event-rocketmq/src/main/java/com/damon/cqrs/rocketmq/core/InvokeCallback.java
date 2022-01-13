package com.damon.cqrs.rocketmq.core;

public interface InvokeCallback {
    void operationComplete(final ResponseFuture responseFuture);
}