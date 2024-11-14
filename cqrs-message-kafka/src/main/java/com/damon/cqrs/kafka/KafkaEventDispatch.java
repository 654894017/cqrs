package com.damon.cqrs.kafka;

import com.damon.cqrs.domain.Event;
import com.damon.cqrs.utils.NamedThreadFactory;
import org.apache.kafka.clients.consumer.KafkaConsumer;
import org.apache.kafka.common.PartitionInfo;

import java.util.List;
import java.util.Properties;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.function.Consumer;

public class KafkaEventDispatch {
    private final ExecutorService executorService;
    private final String topic;
    private final Consumer<List<List<Event>>> consumer;
    private final Properties kafkaConsumerProperties;
    private final int threadSize;

    public KafkaEventDispatch(Properties kafkaConsumerProperties, String topic, Consumer<List<List<Event>>> consumer) {
        this.consumer = consumer;
        this.kafkaConsumerProperties = kafkaConsumerProperties;
        this.topic = topic;
        KafkaConsumer kafkaConsumer = new KafkaConsumer<>(kafkaConsumerProperties);
        List<PartitionInfo> partitionInfos = kafkaConsumer.partitionsFor(topic);
        this.threadSize = partitionInfos.size();
        executorService = Executors.newFixedThreadPool(
                threadSize, new NamedThreadFactory("event-thread-")
        );
    }

    public void start() {
        for (int i = 0; i < threadSize; i++) {
            executorService.submit(new KafkaMessageHandler(consumer, topic, kafkaConsumerProperties));
        }
    }
}