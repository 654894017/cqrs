package com.damon.cqrs.kafka;

import com.damon.cqrs.domain.Event;
import com.damon.cqrs.utils.NamedThreadFactory;
import org.apache.kafka.clients.consumer.KafkaConsumer;
import org.apache.kafka.common.PartitionInfo;
import org.apache.kafka.common.TopicPartition;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Properties;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.function.Consumer;

public class KafkaEventDispatch {
    private final ExecutorService executorService;
    private final List<TopicPartition> partitions;
    private final Consumer<List<List<Event>>> consumer;
    private final Properties kafkaConsumerProperties;

    public KafkaEventDispatch(Properties kafkaConsumerProperties, String topic, Consumer<List<List<Event>>> consumer) {
        this.consumer = consumer;
        this.kafkaConsumerProperties = kafkaConsumerProperties;
        this.partitions = new ArrayList<>();
        KafkaConsumer kafkaConsumer = new KafkaConsumer<>(kafkaConsumerProperties);
        kafkaConsumer.subscribe(Arrays.asList(topic));
        List<PartitionInfo> partitionInfos = kafkaConsumer.partitionsFor(topic);
        partitionInfos.forEach(partitionInfo -> {
            partitions.add(new TopicPartition(partitionInfo.topic(), partitionInfo.partition()));
        });
        executorService = Executors.newFixedThreadPool(
                partitions.size(), new NamedThreadFactory("event-thread-")
        );
    }

    public void start() {
        // 为每个分区分配一个消费者线程
        for (TopicPartition partition : partitions) {
            executorService.submit(new KafkaPartitionHandler(consumer, partition, kafkaConsumerProperties));
        }
    }
}