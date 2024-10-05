package com.damon.cqrs.kafka;

import com.alibaba.fastjson.JSONObject;
import com.damon.cqrs.event.EventSendingContext;
import com.damon.cqrs.event.ISendMessageService;
import com.damon.cqrs.kafka.config.KafkaProducerConfig;
import lombok.extern.slf4j.Slf4j;
import org.apache.kafka.clients.producer.KafkaProducer;
import org.apache.kafka.clients.producer.ProducerRecord;

import java.util.List;
import java.util.Properties;


@Slf4j
public class KafkaSendService implements ISendMessageService {
    private final String topic;
    private final KafkaProducer<String, String> kafkaProducer;

    public KafkaSendService(String topic, String bootstrapServers) {
        this.topic = topic;
        Properties properties = new Properties();
        properties.put("bootstrap.servers", bootstrapServers);
        properties.put("acks", "all");
        properties.put("retries", 0);
        properties.put("batch.size", 16384);
        properties.put("linger.ms", 20);
        properties.put("buffer.memory", 33554432);
        properties.put("key.serializer", "org.apache.kafka.common.serialization.StringSerializer");
        properties.put("value.serializer", "org.apache.kafka.common.serialization.StringSerializer");
        kafkaProducer = new KafkaProducer<>(properties);
    }

    public KafkaSendService(KafkaProducerConfig config) {
        this.topic = config.getTopic();
        this.kafkaProducer = new KafkaProducer<>(config.producerProperties());
    }

    @Override
    public void sendMessage(List<EventSendingContext> contexts) {
        contexts.forEach(context -> {
            kafkaProducer.send(new ProducerRecord<>(topic, context.getAggregateId() + "", JSONObject.toJSONString(context.getEvents())));
        });
        kafkaProducer.flush();
    }
}