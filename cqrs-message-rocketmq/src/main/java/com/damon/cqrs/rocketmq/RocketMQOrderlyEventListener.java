package com.damon.cqrs.rocketmq;

import com.alibaba.fastjson.JSONArray;
import com.alibaba.fastjson.JSONObject;
import com.damon.cqrs.domain.Event;
import com.damon.cqrs.event.IEventListener;
import com.damon.cqrs.utils.ReflectUtils;
import com.damon.cqrs.utils.ThreadUtils;
import lombok.extern.slf4j.Slf4j;
import org.apache.rocketmq.client.consumer.DefaultMQPushConsumer;
import org.apache.rocketmq.client.consumer.listener.ConsumeOrderlyStatus;
import org.apache.rocketmq.client.consumer.listener.MessageListenerOrderly;
import org.apache.rocketmq.client.exception.MQClientException;
import org.apache.rocketmq.common.consumer.ConsumeFromWhere;
import org.apache.rocketmq.common.message.MessageExt;

import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;

/**
 * RocketMQ领域事件监听器（只允许顺序处理的场景使用）
 *
 * @author xianpinglu
 */
@Slf4j
public abstract class RocketMQOrderlyEventListener implements IEventListener {

    public RocketMQOrderlyEventListener(String nameServer, String topic, String consumerGroup, int minThread, int maxThread, int pullBatchSize) throws MQClientException {
        this(nameServer, topic, consumerGroup, minThread, maxThread, pullBatchSize, ConsumeFromWhere.CONSUME_FROM_LAST_OFFSET);
    }

    public RocketMQOrderlyEventListener(String nameServer, String topic, String consumerGroup, int minThread, int maxThread, int pullBatchSize, ConsumeFromWhere where) throws MQClientException {

        DefaultMQPushConsumer consumer = new DefaultMQPushConsumer(consumerGroup);
        consumer.setNamesrvAddr(nameServer);
        consumer.setConsumeFromWhere(where);
        consumer.subscribe(topic, "*");
        consumer.setConsumeThreadMax(maxThread);
        consumer.setConsumeThreadMin(minThread);
        consumer.setPullBatchSize(pullBatchSize);
        consumer.setConsumeMessageBatchMaxSize(pullBatchSize);
        consumer.registerMessageListener((MessageListenerOrderly) (msgs, context) -> {
            try {
                List<List<Event>> events = new ArrayList<>();
                for (MessageExt record : msgs) {
                    String body = new String(record.getBody(), StandardCharsets.UTF_8);
                    JSONArray arrayEvents = JSONObject.parseArray(body);
                    List<Event> eventList = new ArrayList<>();
                    arrayEvents.forEach(e -> {
                        JSONObject json = (JSONObject) e;
                        Event event = JSONObject.parseObject(json.toString(), ReflectUtils.getClass(json.getString("eventType")));
                        eventList.add(event);
                    });
                    events.add(eventList);
                }
                this.process(events);
                return ConsumeOrderlyStatus.SUCCESS;
            } catch (Throwable e) {
                log.error("process domain event failed", e);
                ThreadUtils.sleep(5000);
                /**
                 * 对于顺序消息，当消费者消费消息失败后，消息队列 RocketMQ 会自动不断进行消息重试（每次间隔时间为 1秒）这时，
                 * 应用会出现消息消费被阻審的情况。因此，在使用顺序消息时，务必保证应用能够及时监控并处理消费失败的情况，
                 * 避免阻寨现象的发生。所以消费顺序消息时，consume消费消息失敗时，
                 * 不能返回reconsume--later.这样会导致乱序、应该返回suspend_currentqueue_amoment,意思是先等一会.一会儿再处理这批消息，而不是放到重试队列里
                 */
                return ConsumeOrderlyStatus.SUSPEND_CURRENT_QUEUE_A_MOMENT;
            }
        });
        consumer.start();
    }

    @Override
    public abstract void process(List<List<Event>> events);

}
