package com.damon.cqrs.disruptor2;

import com.lmax.disruptor.EventFactory;

class RpcEventFactory implements EventFactory<RpcEvent> {
    @Override
    public RpcEvent newInstance() {
        return new RpcEvent();
    }
}
