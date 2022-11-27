package com.damon.cqrs.rocketmq;

import com.alibaba.fastjson.JSONArray;
import com.alibaba.fastjson.JSONObject;
import com.damon.cqrs.domain.Event;
import com.damon.cqrs.event.IEventListener;
import com.damon.cqrs.utils.ReflectUtils;
import com.damon.cqrs.utils.ThreadUtils;
import lombok.extern.slf4j.Slf4j;
import org.apache.rocketmq.client.consumer.DefaultMQPushConsumer;
import org.apache.rocketmq.client.consumer.listener.ConsumeConcurrentlyStatus;
import org.apache.rocketmq.client.consumer.listener.MessageListenerConcurrently;
import org.apache.rocketmq.client.exception.MQClientException;
import org.apache.rocketmq.common.consumer.ConsumeFromWhere;
import org.apache.rocketmq.common.message.MessageExt;

import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * RocketMQ领域事件监听器
 *
 * @author xianpinglu
 */
@Slf4j
public abstract class RocketMQConcurrentlyEventListener implements IEventListener {

    public RocketMQConcurrentlyEventListener(String nameServer, String topic, String consumerGroup, int minThread, int maxThread, int pullBatchSize) throws MQClientException {
        this(nameServer, topic, consumerGroup, minThread, maxThread, pullBatchSize, ConsumeFromWhere.CONSUME_FROM_LAST_OFFSET);
    }

    public RocketMQConcurrentlyEventListener(String nameServer, String topic, String consumerGroup, int minThread, int maxThread, int pullBatchSize, ConsumeFromWhere where) throws MQClientException {

        DefaultMQPushConsumer consumer = new DefaultMQPushConsumer(consumerGroup);
        consumer.setNamesrvAddr(nameServer);
        consumer.setConsumeFromWhere(where);
        consumer.subscribe(topic, "*");
        consumer.setConsumeThreadMax(maxThread);
        consumer.setConsumeThreadMin(minThread);
        consumer.setPullBatchSize(pullBatchSize);
        consumer.setConsumeMessageBatchMaxSize(pullBatchSize);
        consumer.registerMessageListener((MessageListenerConcurrently) (msgs, context) -> {
            try {
                Map<String, List<Event>> events = groupAggregateEvent(msgs);
                this.process(events);
                return ConsumeConcurrentlyStatus.CONSUME_SUCCESS;
            } catch (Throwable e) {
                log.error("process domain event failed", e);
                ThreadUtils.sleep(2000);
                return ConsumeConcurrentlyStatus.RECONSUME_LATER;
            }
        });
        consumer.start();
    }

    @Override
    public abstract void process(Map<String, List<Event>> aggregateEventGroup);

    private Map<String, List<Event>> groupAggregateEvent(List<MessageExt> msgs) {
        List<Event> list = new ArrayList<>();
        for (MessageExt message : msgs) {
            String body = new String(message.getBody(), StandardCharsets.UTF_8);
            if (log.isDebugEnabled()) {
                log.debug("received doamin event message. body : {}", body);
            }
            JSONArray events = JSONObject.parseArray(body);
            events.forEach(e -> {
                JSONObject json = (JSONObject) e;
                Event event = JSONObject.parseObject(json.toString(), ReflectUtils.getClass(json.getString("eventType")));
                list.add(event);
            });
        }
        //对同一聚合类型进行分组排序，消费端可以批量处理，提升处理速度。
        Map<String, List<Event>> events = list.stream().collect(Collectors.groupingBy(
                Event::getAggregateType,
                LinkedHashMap::new,
                Collectors.toCollection(ArrayList::new)
        ));
        return events;
    }
}