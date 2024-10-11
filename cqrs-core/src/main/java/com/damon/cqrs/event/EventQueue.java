package com.damon.cqrs.event;

import cn.hutool.core.thread.NamedThreadFactory;
import com.damon.cqrs.command.CommandContext;
import com.damon.cqrs.domain.Command;
import com.lmax.disruptor.EventHandler;
import com.lmax.disruptor.RingBuffer;
import com.lmax.disruptor.dsl.Disruptor;
import com.lmax.disruptor.dsl.ProducerType;
import lombok.Data;

import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentHashMap;

public class EventQueue {
    private static Map<Long, CompletableFuture<Boolean>> map = new ConcurrentHashMap<>();

    public static void main(String[] args) throws InterruptedException {
        final CommandContextFactory factory = new CommandContextFactory();

        final int bufferSize = 2;
        // 创建 Disruptor
        Disruptor<CommandContext> disruptor = new Disruptor<>(factory, bufferSize,
                new NamedThreadFactory("disruptor-thread-pool-", true),
                ProducerType.MULTI, new com.lmax.disruptor.BusySpinWaitStrategy()
        );
        disruptor.handleEventsWith(new EventHandler<>() {
            @Override
            public void onEvent(CommandContext context, long sequence, boolean endOfBatch) throws Exception {
                System.out.println(context);
                Thread.sleep(1000);
                try{
                    context.getFuture().complete(false);
                }finally {
                    map.remove(sequence);
                }
            }
        });

        disruptor.start();
        RingBuffer<CommandContext> ringBuffer = disruptor.getRingBuffer();

        for (int i=0;i<100;i++){
            CompletableFuture<Boolean> ff = publish(ringBuffer, new GoodsCreateCmd(1L, 1L));
            Boolean result = ff.join();
            System.out.println(result);
        }
    }


    public static <R> CompletableFuture<R> publish(RingBuffer<CommandContext> ringBuffer, Command command){
        long sequence = ringBuffer.next();
        try {
            CommandContext context = ringBuffer.get(sequence);
            context.setCommand(command);
            map.put(sequence, context.getFuture());
            return context.getFuture();
        } finally {
            ringBuffer.publish(sequence);
        }
    }
    public static class GoodsCreateCmd extends Command{
        private Long goodsId;

        public GoodsCreateCmd(Long commandId, Long aggregateId) {
            super(commandId, aggregateId);
            this.goodsId = aggregateId;
        }
    }

}
