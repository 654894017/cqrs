package com.damon.cqrs.kafka.config;

import lombok.Data;

import java.util.Properties;

@Data
public class KafkaConsumerConfig {
    private String groupId;
    private String bootstrapServers;
    private String topic;

    public Properties consumerProperties() {
        Properties props = new Properties();
        props.put("bootstrap.servers", bootstrapServers);
        props.put("group.id", groupId);
        props.put("enable.auto.commit", "false");
        props.put("max.poll.records", 1024);
        /**
         * max.poll.interval.ms默认值是5分钟，如果需要加大时长就需要给这个参数重新赋值
         *
         * 这里解释下自己为什么要修改这个参数：因为第一次接收kafka数据，需要加载一堆基础数据，大概执行时间要8分钟，
         * 而5分钟后，kafka认为我没消费，又重新发送，导致我这边收到许多重复数据，所以我需要调大这个值，避免接收重复数据
         */
        props.put("max.poll.interval.ms", "300000");
        props.put("session.timeout.ms", "300000");
        //props.put("auto.commit.interval.ms", "0");
        props.put("key.deserializer", "org.apache.kafka.common.serialization.StringDeserializer");
        props.put("value.deserializer", "org.apache.kafka.common.serialization.StringDeserializer");
        /**
         * 如果存在已经提交的offest时,不管设置为earliest 或者latest 都会从已经提交的offest处开始消费
         * 如果不存在已经提交的offest时,earliest 表示从头开始消费,latest 表示从最新的数据消费,也就是新产生的数据.
         * none topic各分区都存在已提交的offset时，从提交的offest处开始消费；只要有一个分区不存在已提交的offset，则抛出异常
         */
        props.put("auto.offset.reset", "earliest");
        return props;
    }

}
