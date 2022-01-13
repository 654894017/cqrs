package com.damon.cqrs.rocketmq;

import com.alibaba.fastjson.JSONArray;
import com.alibaba.fastjson.JSONObject;
import com.damon.cqrs.IEventListener;
import com.damon.cqrs.domain.Event;
import com.damon.cqrs.utils.ReflectUtils;
import lombok.extern.slf4j.Slf4j;
import org.apache.rocketmq.client.consumer.DefaultMQPushConsumer;
import org.apache.rocketmq.client.consumer.listener.ConsumeOrderlyContext;
import org.apache.rocketmq.client.consumer.listener.ConsumeOrderlyStatus;
import org.apache.rocketmq.client.consumer.listener.MessageListenerOrderly;
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
public abstract class RocketMQEventListener implements IEventListener {

    public RocketMQEventListener(String nameServer, String topic, String consumerGroup, int minThread, int maxThread, int pullBatchSize) throws MQClientException {
        DefaultMQPushConsumer consumer = new DefaultMQPushConsumer(consumerGroup);
        consumer.setNamesrvAddr(nameServer);
        consumer.setConsumeFromWhere(ConsumeFromWhere.CONSUME_FROM_LAST_OFFSET);

        consumer.subscribe(topic, "*");
        consumer.setConsumeThreadMax(maxThread);
        consumer.setConsumeThreadMin(minThread);
        consumer.setPullBatchSize(pullBatchSize);
        consumer.setConsumeMessageBatchMaxSize(pullBatchSize);

        consumer.registerMessageListener((MessageListenerOrderly) (msgs, context) -> {
            try {
                List<Event> list = new ArrayList<>();
                for (MessageExt message : msgs) {
                    String body = new String(message.getBody(), StandardCharsets.UTF_8);
                    log.info("received doamin event message. body : {}", body);
                    JSONArray events = JSONObject.parseArray(body);
                    events.forEach(e -> {
                        JSONObject json = (JSONObject) e;
                        Event event = JSONObject.parseObject(json.toString(), ReflectUtils.getClass(json.getString("eventType")));
                        list.add(event);
                    });
                }
                //对同一聚合根进行分组排序，消费端可以批量处理，提升处理速度。
                Map<String, List<Event>> events = list.stream().collect(Collectors.groupingBy(
                        Event::getAggregateType,
                        LinkedHashMap::new,
                        Collectors.toCollection(ArrayList::new)
                ));
                process(events);
                return ConsumeOrderlyStatus.SUCCESS;
            } catch (Throwable e) {
                log.error("handle domain event failed", e);
                return ConsumeOrderlyStatus.SUSPEND_CURRENT_QUEUE_A_MOMENT;
            }
        });
        consumer.start();
    }

    @Override
    public abstract void process(Map<String, List<Event>> events);

}
