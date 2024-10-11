package com.damon.cqrs.disruptor;

import com.damon.cqrs.command.Handler;
import com.lmax.disruptor.BatchEventProcessor;
import com.lmax.disruptor.EventHandler;

import java.util.concurrent.CompletableFuture;
import java.util.concurrent.atomic.AtomicBoolean;

class RpcProcessor implements EventHandler<RpcEvent> {
    private final Handler handler;

    private final AtomicBoolean isRunning = new AtomicBoolean(true);

    public RpcProcessor(Handler handler) {
        this.handler = handler;
    }

    public void pause(){
        isRunning.compareAndSet(true, false);
    }

    public boolean run(){
        return isRunning.compareAndSet(false, true);
    }

    @Override
    public void onEvent(RpcEvent event, long sequence, boolean endOfBatch) {
        if (isRunning.get()) {
            this.invoke(event);
        } else {
            for (; ; ) {
                if(isRunning.get()){
                    this.invoke(event);
                }
                try {
                    Thread.sleep(10);
                } catch (InterruptedException e) {
                }
            }
        }
    }

    private void invoke(RpcEvent event) {
        CompletableFuture resultFuture = handler.invoke(event.getRequest(), event.getFunction());
        event.getResponseFuture().complete(resultFuture);
    }
}
