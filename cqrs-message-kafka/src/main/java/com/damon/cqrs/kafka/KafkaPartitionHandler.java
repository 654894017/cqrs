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
import org.apache.kafka.common.TopicPartition;

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
public class KafkaPartitionHandler implements Runnable {
    private final TopicPartition partition;
    private final Properties props;
    private final Consumer<List<List<Event>>> consumer;

    public KafkaPartitionHandler(Consumer<List<List<Event>>> consumer, TopicPartition partition, Properties props) {
        this.partition = partition;
        this.props = props;
        this.consumer = consumer;
    }

    @Override
    public void run() {
        // 创建 Kafka 消费者
        KafkaConsumer<String, String> kafkaConsumer = new KafkaConsumer<>(props);
        // 手动分配消费者到特定的分区
        kafkaConsumer.assign(Collections.singletonList(partition));
        for (; ; ) {
            try {
                // 从 Kafka 中拉取消息，超时时间为 1000 毫秒
                ConsumerRecords<String, String> records = kafkaConsumer.poll(Duration.ofMillis(1000));
                if (records.isEmpty()) {
                    continue;
                }
                long offset = records.records(partition).get(0).offset();
                try {
                    List<List<Event>> events = parseEventsFromRecords(records);
                    // 顺序处理每条消息
                    consumer.accept(events);
                    // 手动提交偏移量
                    kafkaConsumer.commitSync();
                    log.info("成功处理: {} 条事件，偏移量为: {}", events.size(), offset);
                } catch (Exception e) {
                    log.error("事件处理失败，偏移量为: {}", offset, e);
                    ThreadUtils.sleep(5000);
                    kafkaConsumer.seek(partition, offset);
                }
            } catch (Exception e) {
                log.error("事件处理失败", e);
            }
        }
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