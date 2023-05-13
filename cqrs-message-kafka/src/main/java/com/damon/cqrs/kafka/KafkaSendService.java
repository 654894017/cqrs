package com.damon.cqrs.kafka;

import com.alibaba.fastjson.JSONObject;
import com.damon.cqrs.event.EventSendingContext;
import com.damon.cqrs.event.ISendMessageService;
import lombok.extern.slf4j.Slf4j;
import org.apache.kafka.clients.producer.ProducerRecord;

import java.util.List;
import java.util.Properties;


@Slf4j
public class KafkaSendService implements ISendMessageService {
    private final String topic;
    private final org.apache.kafka.clients.producer.KafkaProducer<String, String> kafkaProducer;

    public KafkaSendService(String topic, String bootstrapServers) {
        this.topic = topic;
        Properties properties = new Properties();
        properties.put("bootstrap.servers", bootstrapServers);
        properties.put("acks", "all");
        properties.put("retries", 0);
        properties.put("batch.size", 16384);
        properties.put("linger.ms", 1);
        properties.put("buffer.memory", 33554432);
        properties.put("key.serializer", "org.apache.kafka.common.serialization.StringSerializer");
        properties.put("value.serializer", "org.apache.kafka.common.serialization.StringSerializer");
        kafkaProducer = new org.apache.kafka.clients.producer.KafkaProducer<String, String>(properties);
    }


//    public static void main(String[] args) {
//        Properties properties = new Properties();
//        properties.put("bootstrap.servers", "10.230.5.244:9092,10.230.4.87:9092,10.230.5.152:9092");
//        properties.put("acks", "all");
//        properties.put("retries", 0);
//        properties.put("batch.size", 16384);
//        properties.put("linger.ms", 1);
//        properties.put("buffer.memory", 33554432);
//        properties.put("key.serializer", "org.apache.kafka.common.serialization.StringSerializer");
//        properties.put("value.serializer", "org.apache.kafka.common.serialization.StringSerializer");
//        org.apache.kafka.clients.producer.KafkaProducer<String, String> kafkaProducer = new org.apache.kafka.clients.producer.KafkaProducer<String, String>(properties);
//        for (int i = 1; i <= 600; i++) {
//            //参数1：topic名, 参数2：消息文本； ProducerRecord多个重载的构造方法
//            kafkaProducer.send(new ProducerRecord<String, String>("test20200519", "message" + i));
//            System.out.println("message" + i);
//        }
//        kafkaProducer.flush();
//        kafkaProducer.close();
//    }

    @Override
    public void sendMessage(List<EventSendingContext> contexts) {
        contexts.forEach(context -> {
            kafkaProducer.send(new ProducerRecord<String, String>(
                    topic, context.getAggregateId() + "", JSONObject.toJSONString(context.getEvents()))
            );
        });
        kafkaProducer.flush();
    }
}