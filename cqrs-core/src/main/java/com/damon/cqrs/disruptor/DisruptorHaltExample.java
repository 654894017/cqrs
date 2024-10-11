package com.damon.cqrs.disruptor;

import com.lmax.disruptor.EventFactory;
import com.lmax.disruptor.EventHandler;
import com.lmax.disruptor.dsl.Disruptor;
import com.lmax.disruptor.dsl.ProducerType;

import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class DisruptorHaltExample {

    // 定义事件类
    public static class MyEvent {
        private long value;

        public long getValue() {
            return value;
        }

        public void setValue(long value) {
            this.value = value;
        }
    }

    // 事件工厂，用于创建事件实例
    public static class MyEventFactory implements EventFactory<MyEvent> {
        @Override
        public MyEvent newInstance() {
            return new MyEvent();
        }
    }

    // 事件处理器
    public static class MyEventHandler implements EventHandler<MyEvent> {
        @Override
        public void onEvent(MyEvent event, long sequence, boolean endOfBatch) {
            System.out.println("Processing event: " + event.getValue());
            try {
                Thread.sleep(1000);
            } catch (InterruptedException e) {
                throw new RuntimeException(e);
            }
        }
    }

    public static void main(String[] args) throws InterruptedException {
        // 创建线程池
        ExecutorService executor = Executors.newCachedThreadPool();

        // 创建事件工厂
        MyEventFactory factory = new MyEventFactory();

        // 指定环形缓冲区的大小
        int bufferSize = 1024;

        // 创建 Disruptor
        Disruptor<MyEvent> disruptor = new Disruptor<>(factory, bufferSize, executor, ProducerType.SINGLE, new com.lmax.disruptor.BlockingWaitStrategy());

        // 连接事件处理器
        disruptor.handleEventsWith(new MyEventHandler());

        // 启动 Disruptor
        disruptor.start();

        // 发布事件
        for (int i = 0; i < 15; i++) {
            long sequence = disruptor.getRingBuffer().next();  // 获取下一个可用的序列号
            try {
                MyEvent event = disruptor.getRingBuffer().get(sequence); // 获取该序列号对应的事件
                event.setValue(i); // 填充事件数据
            } finally {
                disruptor.getRingBuffer().publish(sequence); // 发布事件
            }
        }

        // 模拟运行一段时间
        Thread.sleep(10000);

        // 使用 halt() 立即停止事件处理器
        System.out.println("Halting Disruptor...");
        disruptor.halt();  // 立即停止事件处理

        Thread.sleep(5000);
        disruptor.start();  // 立即停止事件处理
        for (int i = 0; i < 15; i++) {
            long sequence = disruptor.getRingBuffer().next();  // 获取下一个可用的序列号
            try {
                MyEvent event = disruptor.getRingBuffer().get(sequence); // 获取该序列号对应的事件
                event.setValue(i); // 填充事件数据
            } finally {
                disruptor.getRingBuffer().publish(sequence); // 发布事件
            }
        }

//
//        // 关闭线程池
//        executor.shutdown();
//        System.out.println("Disruptor halted.");
    }
}