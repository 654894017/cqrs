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
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.function.Function;

public class DisruptorRpc2 {
    private List<RingBuffer<RpcEvent>> ringBufferList = new ArrayList<>();
    private int size = 1;

    public DisruptorRpc2(Handler handler) {
        RpcEventFactory factory = new RpcEventFactory();
        NamedThreadFactory namedThreadFactory = new NamedThreadFactory("disruptor-thread-pool-", false);
        for (int i = 0; i < size; i++) {
            Disruptor<RpcEvent> disruptor = new Disruptor<>(factory, 1024, namedThreadFactory,
                    ProducerType.MULTI, new com.lmax.disruptor.BlockingWaitStrategy()
            );
            disruptor.handleEventsWith(new RpcProcessor(handler));
            disruptor.start();
            RingBuffer<RpcEvent> ringBuffer = disruptor.getRingBuffer();
            ringBufferList.add(ringBuffer);
        }
    }

    public static void main(String[] args) throws Exception {
        DisruptorRpc2 disruptorRpc = new DisruptorRpc2(new Handler() {
            @Override
            public CompletableFuture invoke(Command command, Function function) {
                return CompletableFuture.completedFuture("Hello");
            }
        });
        CountDownLatch latch = new CountDownLatch(256);
        ExecutorService executorService = Executors.newVirtualThreadPerTaskExecutor();
        long startTime = System.currentTimeMillis();
        for(int k=0; k< 256;k++){
            int finalK = k;
            executorService.submit(()->{
                List<CompletableFuture> list = new ArrayList<>();
                // 模拟多个 RPC 调用
                for (int i = 0; i < 500000; i++) {
                    GoodsCreateCmd cmd = new GoodsCreateCmd(1L, (long) finalK);
                    CompletableFuture<CompletableFuture> responseFuture = disruptorRpc.call(cmd, new Function() {
                        @Override
                        public CompletableFuture apply(Object object) {
                            return CompletableFuture.completedFuture("success");
                        }
                    });
                    CompletableFuture future = responseFuture.thenAccept(response -> {
                        //System.out.println("Received: " + response.join());
                        //latch.countDown();
                    });
                    list.add(future);
                }
                //System.out.println(list.size());
                CompletableFuture.allOf(list.toArray(new CompletableFuture[0])).join();
                latch.countDown();
            });
        }
        latch.await();
        System.out.println("Total time: " + (System.currentTimeMillis() - startTime));
    }

    private RingBuffer<RpcEvent> getRingBuffer(Long aggregateId) {
        int hash = aggregateId.hashCode();
        if (hash < 0) {
            hash = Math.abs(hash);
        }
        int index = hash % size;
        return ringBufferList.get(index);
    }

    public <R> CompletableFuture<R> call(Command request, Function function) {
        RingBuffer<RpcEvent> ringBuffer = getRingBuffer(request.getAggregateId());
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
}