package com.damon.cqrs.config;

import cn.hutool.core.thread.NamedThreadFactory;
import com.damon.cqrs.command.CommandContext;
import com.damon.cqrs.command.Handler;
import com.damon.cqrs.domain.Command;
import com.damon.cqrs.event.CommandContextFactory;
import com.lmax.disruptor.EventHandler;
import com.lmax.disruptor.RingBuffer;
import com.lmax.disruptor.dsl.Disruptor;
import com.lmax.disruptor.dsl.ProducerType;
import lombok.Data;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Function;

/**
 * 聚合根锁
 */
@Data
public class DisruptorConfig {
    private final List<RingBuffer> ringBufferList = new ArrayList<>();
    private final int lockNumber;
    private final Map<Long, CompletableFuture<?>> map = new ConcurrentHashMap<>();
//    RingBuffer<CommandContext> ringBuffer;
    public DisruptorConfig(int lockNumber, Handler handler) {
        this.lockNumber = lockNumber;
        CommandContextFactory factory = new CommandContextFactory();
        NamedThreadFactory namedThreadFactory = new NamedThreadFactory("disruptor-thread-pool-", false);
        for (int i = 0; i < lockNumber; i++) {
            Disruptor<CommandContext> disruptor = new Disruptor<>(factory, 1024, namedThreadFactory,
                    ProducerType.MULTI, new com.lmax.disruptor.BusySpinWaitStrategy()
            );
            ringBufferList.add(disruptor.getRingBuffer());
            disruptor.handleEventsWith(new EventHandler<CommandContext>() {
                @Override
                public void onEvent(CommandContext event, long sequence, boolean endOfBatch) throws Exception {
                try{
                    CompletableFuture future1 = map.get(sequence);
                    CompletableFuture<?> future = handler.invoke(event.getCommand(), (Function<Object, Object>) event.getCallback());
                    future1.complete(future);
                }finally {
                    map.remove(sequence);
                }
                }
            });
            disruptor.start();
        }
//        this.lockNumber = lockNumber;
//        CommandContextFactory factory = new CommandContextFactory();
//        NamedThreadFactory namedThreadFactory = new NamedThreadFactory("disruptor-thread-pool-", false);
//        Disruptor<CommandContext> disruptor = new Disruptor<>(factory, 1024 * 1024, namedThreadFactory,
//                ProducerType.MULTI, new com.lmax.disruptor.BusySpinWaitStrategy()
//        );
//        ringBufferList.add(disruptor.getRingBuffer());
//        disruptor.handleEventsWith(new EventHandler<CommandContext>() {
//            @Override
//            public void onEvent(CommandContext event, long sequence, boolean endOfBatch) throws Exception {
//                try{
//                    CompletableFuture future1 = map.get(sequence);
//                    CompletableFuture<?> future = handler.invoke(event.getCommand(), (Function<Object, Object>) event.getCallback());
//                    future1.complete(future);
//                }finally {
//                    map.remove(sequence);
//                }
//            }
//        });
//        disruptor.start();
//
//        ringBuffer = disruptor.getRingBuffer();

    }

    private RingBuffer<CommandContext> getLock(Long aggregateId) {
        int hash = aggregateId.hashCode();
        if (hash < 0) {
            hash = Math.abs(hash);
        }
        int index = hash % lockNumber;
        return ringBufferList.get(index);
    }

    public <T, R> CompletableFuture<R> invoke(Command command, Function<T, R> function) {
        RingBuffer<CommandContext> ringBuffer = getLock(command.getAggregateId());
        long sequence = ringBuffer.next();
        CommandContext context = ringBuffer.get(sequence);
        try {
            context.setCommand(command);
            context.setCallback(function);
            map.put(sequence, context.getFuture());
        } finally {
            ringBuffer.publish(sequence);
        }
        return (CompletableFuture<R>) context.getFuture().join();

    }
}
