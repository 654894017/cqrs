package com.damon.cqrs.kafka;

import com.damon.cqrs.domain.Event;
import com.damon.cqrs.event.IEventListener;
import com.damon.cqrs.utils.NamedThreadFactory;
import lombok.extern.slf4j.Slf4j;
import org.apache.kafka.clients.consumer.KafkaConsumer;

import java.util.List;
import java.util.Map;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

/**
 * kafka消费者
 */
@Slf4j
public abstract class KafkaEventListener implements IEventListener {

    private KafkaConsumer<String, String> kafkaConsumer;

    public KafkaEventListener(String topic, String groupId, int threadNumber, String bootstrapServers) {
        ExecutorService processService = Executors.newFixedThreadPool(threadNumber, new NamedThreadFactory("mq-cqrs-consumer-pool"));
        for (int i = 0; i < threadNumber; i++) {
            processService.submit(new ConsumerRunnable(topic, groupId, bootstrapServers, this::process));
        }
    }

    @Override
    public abstract void process(Map<Integer, List<List<Event>>> aggregateEventGroup);
}