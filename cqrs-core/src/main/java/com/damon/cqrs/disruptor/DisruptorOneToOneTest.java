package com.damon.cqrs.disruptor;

import com.lmax.disruptor.*;
import com.lmax.disruptor.dsl.Disruptor;
import com.lmax.disruptor.dsl.ProducerType;

import java.nio.ByteBuffer;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

// 定义事件(Event)
class LongEvent {
    private long value;

    public long getValue() {
        return value;
    }

    public void setValue(long value) {
        this.value = value;
    }
}

// 定义事件工厂(EventFactory)
class LongEventFactory implements EventFactory<LongEvent> {
    @Override
    public LongEvent newInstance() {
        return new LongEvent();
    }
}

// 定义事件处理器(EventHandler)
class LongEventHandler implements EventHandler<LongEvent> {
    @Override
    public void onEvent(LongEvent event, long sequence, boolean endOfBatch) {
        // 消费者逻辑
        // System.out.println("Event: " + event.getValue());
    }
}

// 定义生产者(Producer)
class LongEventProducer {
    private final RingBuffer<LongEvent> ringBuffer;

    public LongEventProducer(RingBuffer<LongEvent> ringBuffer) {
        this.ringBuffer = ringBuffer;
    }

    public void onData(ByteBuffer bb) {
        long sequence = ringBuffer.next();  // 获取下一个可用的序列号
        try {
            LongEvent event = ringBuffer.get(sequence); // 获取该序列号对应的事件
            event.setValue(bb.getLong(0));  // 填充事件数据
        } finally {
            ringBuffer.publish(sequence); // 发布事件
        }
    }
}

public class DisruptorOneToOneTest {
    public static void main(String[] args) throws InterruptedException {
        // 创建事件工厂
        LongEventFactory factory = new LongEventFactory();

        // 定义环形缓冲区大小（必须是2的幂次方）
        int bufferSize = 1024 * 1024;

        // 创建线程池来为消费者提供服务
        ExecutorService executor = Executors.newCachedThreadPool();

        // 创建 Disruptor 实例
        Disruptor<LongEvent> disruptor = new Disruptor<>(factory, bufferSize, executor, ProducerType.SINGLE, new BusySpinWaitStrategy());

        // 连接事件处理器
        disruptor.handleEventsWith(new LongEventHandler());

        // 启动 Disruptor
        disruptor.start();

        // 获取 RingBuffer 用来发布事件
        RingBuffer<LongEvent> ringBuffer = disruptor.getRingBuffer();

        // 创建生产者
        LongEventProducer producer = new LongEventProducer(ringBuffer);

        // 模拟生成事件的数量为 1000 万
        ByteBuffer bb = ByteBuffer.allocate(8);
        long startTime = System.currentTimeMillis();
        for (long l = 0; l < 10_00000_000; l++) {
            bb.putLong(0, l);
            producer.onData(bb);
        }

        long endTime = System.currentTimeMillis();
        System.out.println("Total time: " + (endTime - startTime) + " ms");

        // 关闭 Disruptor
        disruptor.shutdown();
        executor.shutdown();
    }
}