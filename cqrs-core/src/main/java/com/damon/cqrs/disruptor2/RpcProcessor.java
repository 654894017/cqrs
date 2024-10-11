package com.damon.cqrs.disruptor2;

import com.lmax.disruptor.EventHandler;

class RpcProcessor implements EventHandler<RpcEvent> {
    @Override
    public void onEvent(RpcEvent event, long sequence, boolean endOfBatch) {
        // 模拟处理请求
        //System.out.println("Processing request: " + event.getRequest());

        // 设置响应
        String response = "Response for: " + event.getRequest();
        event.getResponseFuture().complete(response); // 完成 CompletableFuture
    }
}
