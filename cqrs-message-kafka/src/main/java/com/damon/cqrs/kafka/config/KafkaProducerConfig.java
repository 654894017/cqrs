package com.damon.cqrs.kafka.config;

import lombok.Data;

import java.util.Properties;

@Data
public class KafkaProducerConfig {
    private String topic;
    private String bootstrapServers;
    private Properties properties;

    public KafkaProducerConfig(String bootstrapServers, String topic) {
        Properties properties = new Properties();
        properties.put("bootstrap.servers", bootstrapServers);
        properties.put("acks", "all");
        properties.put("retries", 0);
        properties.put("batch.size", 16384);
        properties.put("linger.ms", 20);
        properties.put("buffer.memory", 33554432);
        properties.put("key.serializer", "org.apache.kafka.common.serialization.StringSerializer");
        properties.put("value.serializer", "org.apache.kafka.common.serialization.StringSerializer");
        this.properties = properties;
        this.bootstrapServers = bootstrapServers;
        this.topic = topic;
    }

}
