package com.damon.cqrs.kafka.config;

import lombok.Data;

import java.util.Properties;

@Data
public class KafkaConsumerConfig {
    private String groupId;
    private String bootstrapServers;
    private String topic;
    private Properties properties;

    public KafkaConsumerConfig(String bootstrapServers, String topic, String groupId) {
        Properties properties = new Properties();
        properties.put("bootstrap.servers", bootstrapServers);
        properties.put("group.id", groupId);
        properties.put("enable.auto.commit", "false");
        properties.put("max.poll.records", 1024);
        /**
         * max.poll.interval.ms默认值是5分钟，如果需要加大时长就需要给这个参数重新赋值
         *
         * 这里解释下自己为什么要修改这个参数：因为第一次接收kafka数据，需要加载一堆基础数据，大概执行时间要8分钟，
         * 而5分钟后，kafka认为我没消费，又重新发送，导致我这边收到许多重复数据，所以我需要调大这个值，避免接收重复数据
         */
        properties.put("max.poll.interval.ms", "1800000");
        /**
         * 默认值是 10000 毫秒（即 10 秒）。
         * session.timeout.ms 必须大于 heartbeat.interval.ms。heartbeat.interval.ms 是消费者向 Kafka 发送心跳的频率，
         * 而 session.timeout.ms 是 Kafka 认为消费者失效的最大等待时间。如果心跳间隔小于会话超时时间，则可能会频繁触发再均衡。
         * 通常，heartbeat.interval.ms 是 session.timeout.ms 的 1/3 或更小。
         *
         * 如果消费者处理消息的时间较长，可能需要适当增加 session.timeout.ms，以避免 Kafka 认为消费者失效。
         * 例如，如果消费者每次处理消息需要 5 秒钟，而 session.timeout.ms 设置为 3 秒，可能会导致消费者被频繁移除。
         */
        properties.put("session.timeout.ms", "60000");
        properties.put("heartbeat.interval.ms", "10000");
        properties.put("key.deserializer", "org.apache.kafka.common.serialization.StringDeserializer");
        properties.put("value.deserializer", "org.apache.kafka.common.serialization.StringDeserializer");
        /**
         * 如果存在已经提交的offest时,不管设置为earliest 或者latest 都会从已经提交的offest处开始消费
         * 如果不存在已经提交的offest时,earliest 表示从头开始消费,latest 表示从最新的数据消费,也就是新产生的数据.
         * none topic各分区都存在已提交的offset时，从提交的offest处开始消费；只要有一个分区不存在已提交的offset，则抛出异常
         */
        properties.put("auto.offset.reset", "earliest");
        this.properties = properties;
        this.topic = topic;
        this.groupId = groupId;
        this.bootstrapServers = bootstrapServers;
    }
}
