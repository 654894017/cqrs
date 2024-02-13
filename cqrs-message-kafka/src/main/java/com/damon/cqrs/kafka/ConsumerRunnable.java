package com.damon.cqrs.kafka;

import com.alibaba.fastjson.JSONArray;
import com.alibaba.fastjson.JSONObject;
import com.damon.cqrs.domain.Event;
import com.damon.cqrs.utils.ReflectUtils;
import com.damon.cqrs.utils.ThreadUtils;
import com.google.common.collect.ImmutableMap;
import lombok.extern.slf4j.Slf4j;
import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.apache.kafka.clients.consumer.ConsumerRecords;
import org.apache.kafka.clients.consumer.KafkaConsumer;
import org.apache.kafka.clients.consumer.OffsetAndMetadata;

import java.time.Duration;
import java.util.*;
import java.util.function.Consumer;

@Slf4j
public class ConsumerRunnable implements Runnable {
    private final KafkaConsumer<String, String> kafkaConsumer;
    private final Consumer<Map<Integer, List<List<Event>>>> consumer;

    public ConsumerRunnable(String topic, String groupId, String bootstrapServers, Consumer<Map<Integer, List<List<Event>>>> consumer) {
        this.consumer = consumer;
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
        props.put("max.poll.interval.ms", "30000");
        props.put("session.timeout.ms", "30000");
        //props.put("auto.commit.interval.ms", "0");
        props.put("key.deserializer", "org.apache.mq.common.serialization.StringDeserializer");
        props.put("value.deserializer", "org.apache.mq.common.serialization.StringDeserializer");
        /**
         * 如果存在已经提交的offest时,不管设置为earliest 或者latest 都会从已经提交的offest处开始消费
         * 如果不存在已经提交的offest时,earliest 表示从头开始消费,latest 表示从最新的数据消费,也就是新产生的数据.
         * none topic各分区都存在已提交的offset时，从提交的offest处开始消费；只要有一个分区不存在已提交的offset，则抛出异常
         */
        props.put("auto.offset.reset", "earliest");
        // KafkaConsumer类不是线程安全的
        kafkaConsumer = new KafkaConsumer<>(props);
        kafkaConsumer.subscribe(Arrays.asList(topic)); // 订阅topic
    }

    @Override
    public void run() {
        try {
            for (; ; ) {
                // 拉取消息
                ConsumerRecords<String, String> topicRecords = kafkaConsumer.poll(Duration.ofMillis(1000));
                topicRecords.partitions().forEach(topicPartition -> {
                    List<ConsumerRecord<String, String>> partionRecords = topicRecords.records(topicPartition);
                    Long startOffset = partionRecords.get(0).offset();
                    Long lastOffset = partionRecords.get(partionRecords.size() - 1).offset();
                    try {
                        List<List<Event>> events = new ArrayList<>();
                        for (ConsumerRecord<String, String> record : partionRecords) {
                            JSONArray arrayEvents = JSONObject.parseArray(record.value());
                            List<Event> eventList = new ArrayList<>();
                            arrayEvents.forEach(e -> {
                                JSONObject json = (JSONObject) e;
                                Event event = JSONObject.parseObject(json.toString(), ReflectUtils.getClass(json.getString("eventType")));
                                eventList.add(event);
                            });
                            events.add(eventList);
                        }
                        consumer.accept(ImmutableMap.of(topicPartition.partition(), events));
                        kafkaConsumer.commitSync(ImmutableMap.of(topicPartition, new OffsetAndMetadata(lastOffset)));
                    } catch (Throwable e) {
                        log.error("process mq event failed", e);
                        // 当分区消息处理失败时，重置offset到消费失败位置，下次消费从当前offset开始消费
                        kafkaConsumer.seek(topicPartition, new OffsetAndMetadata(startOffset));
                        ThreadUtils.sleep(10000);
                    }
                });
            }
        } catch (Throwable e) {
            log.error(e.getMessage(), e);
        }
    }

}
