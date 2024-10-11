package com.damon.cqrs.disruptor2;

import cn.hutool.core.thread.NamedThreadFactory;
import com.lmax.disruptor.RingBuffer;
import com.lmax.disruptor.dsl.Disruptor;
import com.lmax.disruptor.dsl.ProducerType;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CountDownLatch;

public class DisruptorRpc {

    private final Disruptor<RpcEvent> disruptor;
    private final RingBuffer<RpcEvent> ringBuffer;

    public DisruptorRpc() {
        RpcEventFactory factory = new RpcEventFactory();
        int bufferSize = 1024;
        NamedThreadFactory namedThreadFactory = new NamedThreadFactory("disruptor-thread-pool-", false);
        disruptor = new Disruptor<>(factory, bufferSize, namedThreadFactory,
                ProducerType.SINGLE, new com.lmax.disruptor.BlockingWaitStrategy()
        );
        // 创建 Disruptor
        disruptor.handleEventsWith(new RpcProcessor());
        disruptor.start();
        // 获取 RingBuffer
        ringBuffer = disruptor.getRingBuffer();
    }

    public CompletableFuture<String> call(String request) {
        CompletableFuture<String> future = new CompletableFuture<>(); // 创建 CompletableFuture
        long sequence = ringBuffer.next(); // 获取下一个可用的序列号
        try {
            RpcEvent event = ringBuffer.get(sequence); // 获取该序列号对应的事件
            event.setRequest(request); // 设置请求内容
            event.setResponseFuture(future); // 设置响应的 CompletableFuture
        } finally {
            ringBuffer.publish(sequence); // 发布事件
        }
        return future; // 返回 CompletableFuture
    }

    public static void main(String[] args) throws Exception {
        DisruptorRpc rpcExample = new DisruptorRpc();
        long startTime = System.currentTimeMillis();
        List<CompletableFuture> list = new ArrayList<>();
        // 模拟多个 RPC 调用
        for (int i = 0; i < 100*500000; i++) {
            CompletableFuture<String> responseFuture = rpcExample.call("Request " + i);

            list.add(responseFuture.thenAccept(response -> {
            }));
        }
        CompletableFuture.allOf(list.toArray(new CompletableFuture[0])).join();
        System.out.println("Total time: " + (System.currentTimeMillis() - startTime));
        // 关闭 Disruptor
        rpcExample.disruptor.shutdown();
    }


//    public static void main(String[] args) throws Exception {
//        DisruptorRpc rpcExample = new DisruptorRpc();
//        CountDownLatch countDownLatch = new CountDownLatch(100* 500000);
//        long startTime = System.currentTimeMillis();
//        for(int k=0;k<100;k++){
//            new Thread(()->{
//                // 模拟多个 RPC 调用
//                for (int i = 0; i < 500000; i++) {
//                    CompletableFuture<String> responseFuture = rpcExample.call("Request " + i);
//                    responseFuture.thenAccept(response -> {
//                        //System.out.println("Received: " + response);
//                        countDownLatch.countDown();
//                    }).join();
//                }
//            }).start();
//        }
//        countDownLatch.await();
//        System.out.println("Total time: " + (System.currentTimeMillis() - startTime));
//        // 关闭 Disruptor
//        rpcExample.disruptor.shutdown();
//    }


//    public static void main(String[] args) throws Exception {
//        DisruptorRpc rpcExample = new DisruptorRpc();
//        long startTime = System.currentTimeMillis();
//        CountDownLatch latch = new CountDownLatch(50000000);
//        for (int i = 0; i < 50000000; i++) {
//            CompletableFuture<String> responseFuture = rpcExample.call("Request " + i);
//            responseFuture.thenAccept(response -> {
//                //System.out.println("Received: " + response);
//                latch.countDown();
//            });
//        }
//        latch.await();
//        System.out.println("Total time: " + (System.currentTimeMillis() - startTime));
//        // 关闭 Disruptor
//        rpcExample.disruptor.shutdown();
//    }
}