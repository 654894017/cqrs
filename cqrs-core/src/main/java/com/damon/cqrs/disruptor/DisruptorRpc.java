package com.damon.cqrs.disruptor;

import cn.hutool.core.thread.NamedThreadFactory;
import com.damon.cqrs.command.Handler;
import com.damon.cqrs.domain.Command;
import com.lmax.disruptor.RingBuffer;
import com.lmax.disruptor.dsl.Disruptor;
import com.lmax.disruptor.dsl.ProducerType;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.function.Function;

public class DisruptorRpc {

    private final Disruptor<RpcEvent> disruptor;
    private final RingBuffer<RpcEvent> ringBuffer;

    public DisruptorRpc(Handler handler) {
        RpcEventFactory factory = new RpcEventFactory();
        NamedThreadFactory namedThreadFactory = new NamedThreadFactory("disruptor-thread-pool-", false);
        disruptor = new Disruptor<>(factory, 1024, namedThreadFactory,
                ProducerType.SINGLE, new com.lmax.disruptor.BlockingWaitStrategy()
        );
        int processorCount = 1;
        RpcProcessor[] handlers = new RpcProcessor[processorCount];
        for (int i = 0; i < processorCount; i++) {
            handlers[i] = new RpcProcessor(handler);
        }
        // 创建 Disruptor
        disruptor.handleEventsWith(handlers);
//        disruptor.handleEventsWith(new RpcProcessor(handler, 0, 0));
        disruptor.start();
        // 获取 RingBuffer
        ringBuffer = disruptor.getRingBuffer();
    }

    public <R> CompletableFuture<R> call(Command request, Function function) {
        // 创建 CompletableFuture
        CompletableFuture<R> future = new CompletableFuture<>();
        // 获取下一个可用的序列号
        long sequence = ringBuffer.next();
        try {
            // 获取该序列号对应的事件
            RpcEvent event = ringBuffer.get(sequence);
            // 设置请求内容
            event.setRequest(request);
            event.setFunction(function);
            // 设置响应的 CompletableFuture
            event.setResponseFuture(future);
        } finally {
            // 发布事件
            ringBuffer.publish(sequence);
        }
        return future;
    }

    public static void main(String[] args) throws Exception {
        DisruptorRpc disruptorRpc = new DisruptorRpc(new Handler() {
            @Override
            public CompletableFuture invoke(Command command, Function function) {
                return CompletableFuture.completedFuture("Hello");
            }
        });
        List<CompletableFuture> list = new ArrayList<>(50000000);
        long startTime = System.currentTimeMillis();
        // 模拟多个 RPC 调用
        for (int i = 0; i < 50000000; i++) {
            GoodsCreateCmd cmd = new GoodsCreateCmd(1L, 1L);
            CompletableFuture<CompletableFuture> responseFuture = disruptorRpc.call(cmd, new Function() {
                @Override
                public CompletableFuture apply(Object object) {
                    return CompletableFuture.completedFuture("success");
                }
            });
            CompletableFuture future = responseFuture.thenAccept(response -> {
               // System.out.println("Received: " + response.join());
            });
            list.add(future);
            // 为了模拟并发，这里可以稍微延迟一下
            //Thread.sleep(100);
        }
        CompletableFuture.allOf(list.toArray(new CompletableFuture[0])).join();
        System.out.println("Total time: " + (System.currentTimeMillis() - startTime));
        // 关闭 Disruptor
        disruptorRpc.disruptor.shutdown();
    }
}