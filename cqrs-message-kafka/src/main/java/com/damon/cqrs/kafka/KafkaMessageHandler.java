package com.damon.cqrs.kafka;

import com.alibaba.fastjson.JSONArray;
import com.alibaba.fastjson.JSONObject;
import com.damon.cqrs.domain.Event;
import com.damon.cqrs.utils.ReflectUtils;
import com.damon.cqrs.utils.ThreadUtils;
import lombok.extern.slf4j.Slf4j;
import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.apache.kafka.clients.consumer.ConsumerRecords;
import org.apache.kafka.clients.consumer.KafkaConsumer;

import java.time.Duration;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Properties;
import java.util.function.Consumer;

/**
 * @author xianpinglu
 */
@Slf4j
public class KafkaMessageHandler implements Runnable {
    private final String topic;
    private final Consumer<List<List<Event>>> consumer;
    private final Properties properties;

    public KafkaMessageHandler(Consumer<List<List<Event>>> consumer, String topic, Properties properties) {
        this.topic = topic;
        this.consumer = consumer;
        this.properties = properties;
    }

    private static void retryFailedMessages(KafkaConsumer<String, String> kafkaConsumer, ConsumerRecords<String, String> records) {
        records.partitions().forEach(partition -> {
            long offset = records.records(partition).get(0).offset();
            kafkaConsumer.seek(partition, offset);
        });
    }

    private static void printCustomerOffset(KafkaConsumer<String, String> kafkaConsumer, ConsumerRecords<String, String> records) {
        records.partitions().forEach(partition -> {
            long offset = records.records(partition).get(0).offset();
            log.info("成功处理分区: {}, 共计: {}条事件，偏移量为: {}", partition, records.records(partition).size(), offset);
        });
    }

    @Override
    public void run() {
        KafkaConsumer<String, String> kafkaConsumer = createConsumer();
        ConsumerRecords<String, String> records = null;
        for (; ; ) {
            try {
                // 从 Kafka 中拉取消息，超时时间为 1000 毫秒
                records = kafkaConsumer.poll(Duration.ofMillis(1000));
                if (records.isEmpty()) {
                    continue;
                }
                List<List<Event>> events = parseEventsFromRecords(records);
                // 顺序处理每条消息
                consumer.accept(events);
                // 手动提交偏移量
                kafkaConsumer.commitSync();
                // 打印分区消费进度日志
                printCustomerOffset(kafkaConsumer, records);
            } catch (Exception e) {
                log.error("事件处理失败", e);
                if (records != null) {
                    retryFailedMessages(kafkaConsumer, records);
                }
                ThreadUtils.sleep(5000);
            }
        }
    }

    private KafkaConsumer<String, String> createConsumer() {
        KafkaConsumer<String, String> kafkaConsumer = new KafkaConsumer<>(properties);
        kafkaConsumer.subscribe(Collections.singletonList(topic));
        return kafkaConsumer;
    }

    private List<List<Event>> parseEventsFromRecords(ConsumerRecords<String, String> records) {
        List<List<Event>> events = new ArrayList<>();
        for (ConsumerRecord<String, String> record : records) {
            JSONArray arrayEvents = JSONObject.parseArray(record.value());
            List<Event> eventList = new ArrayList<>();
            arrayEvents.forEach(e -> {
                JSONObject json = (JSONObject) e;
                Event event = JSONObject.parseObject(json.toString(), ReflectUtils.getClass(json.getString("eventType")));
                eventList.add(event);
            });
            events.add(eventList);
        }
        return events;
    }

}