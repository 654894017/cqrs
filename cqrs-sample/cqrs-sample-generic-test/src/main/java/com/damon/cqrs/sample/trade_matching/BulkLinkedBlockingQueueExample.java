package com.damon.cqrs.sample.trade_matching;

import java.util.concurrent.LinkedBlockingQueue;

public class BulkLinkedBlockingQueueExample {
    private static final int QUEUE_CAPACITY = 100; // 队列容量
    private static final LinkedBlockingQueue<Integer> queue = new LinkedBlockingQueue<>();
    private static final int TOTAL_ITEMS = 1000000; // 生产和消费的总数量

    public static void main(String[] args) {
        // 创建并启动生产者线程
        Thread producer = new Thread(new Producer());
        producer.start();

        // 创建并启动消费者线程
        Thread consumer = new Thread(new Consumer());
        consumer.start();
    }

    // 生产者类
    static class Producer implements Runnable {
        @Override
        public void run() {
            try {
                for (int i = 0; i < TOTAL_ITEMS; i++) {
                    queue.put(i); // 将数据放入队列
                    if (i % 10000 == 0) { // 每生产 10000 项输出一次
                        System.out.println("Produced: " + i);
                    }
                }
                System.out.println("Producer finished producing.");
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                System.out.println("Producer was interrupted");
            }
        }
    }

    // 消费者类
    static class Consumer implements Runnable {
        @Override
        public void run() {
            try {
                for (int i = 0; i < TOTAL_ITEMS; i++) {
                    Integer value = queue.take(); // 从队列中获取数据
                    if (value % 10000 == 0) { // 每消费 10000 项输出一次
                        System.out.println("Consumed: " + value);
                    }
                }
                System.out.println("Consumer finished consuming.");
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                System.out.println("Consumer was interrupted");
            }
        }
    }
}