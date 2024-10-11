package com.damon.cqrs.disruptor;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.*;

class RpcRequest {
    private final String request;
    private final CompletableFuture<String> responseFuture;

    public RpcRequest(String request) {
        this.request = request;
        this.responseFuture = new CompletableFuture<>();
    }

    public String getRequest() {
        return request;
    }

    public CompletableFuture<String> getResponseFuture() {
        return responseFuture;
    }
}

class RpcServer implements Runnable {
    private final LinkedBlockingQueue<RpcRequest> requestQueue;

    public RpcServer(LinkedBlockingQueue<RpcRequest> requestQueue) {
        this.requestQueue = requestQueue;
    }

    @Override
    public void run() {
        try {
            while (true) {
                // 从队列中取出请求并处理
                RpcRequest rpcRequest = requestQueue.take();
//                System.out.println("Server: Processing request - " + rpcRequest.getRequest());
//
//                // 模拟处理时间
//                Thread.sleep(1000);

                // 返回处理结果
                String response = "Response for: " + rpcRequest.getRequest();
                rpcRequest.getResponseFuture().complete(response);
            }
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
    }
}

class RpcClient {
    private final LinkedBlockingQueue<RpcRequest> requestQueue;

    public RpcClient(LinkedBlockingQueue<RpcRequest> requestQueue) {
        this.requestQueue = requestQueue;
    }

    public CompletableFuture<String> call(String request) {
        RpcRequest rpcRequest = new RpcRequest(request);
        try {
            // 将请求放入队列
            requestQueue.put(rpcRequest);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }

        // 返回一个 CompletableFuture，异步等待响应
        return rpcRequest.getResponseFuture();
    }
}

public class LinkedBlockingQueueRpcExample {

    public static void main(String[] args) throws Exception {
        LinkedBlockingQueue<RpcRequest> requestQueue = new LinkedBlockingQueue<>();

        // 启动服务器线程
        RpcServer server = new RpcServer(requestQueue);
        Thread serverThread = new Thread(server);
        serverThread.start();

        // 创建客户端
        RpcClient client = new RpcClient(requestQueue);
        List<CompletableFuture> list = new ArrayList<>();

        long startTime = System.currentTimeMillis();
        // 模拟多个 RPC 调用
        for (int i = 0; i < 50000000; i++) {
            String request = "Request " + i;
            CompletableFuture<String> responseFuture = client.call(request);

            // 异步接收响应
            CompletableFuture future = responseFuture.thenAccept(response -> {
               // System.out.println("Client: Received response - " + response);
            });
            list.add(future);
//            // 模拟并发
//            Thread.sleep(500);
        }
        CompletableFuture.allOf(list.toArray(new CompletableFuture[0])).join();
        System.out.println("Total time: " + (System.currentTimeMillis() - startTime));

        // 主线程等待一段时间以确保所有任务完成
        Thread.sleep(5000);

        // 停止服务器线程
        serverThread.interrupt();
    }
}