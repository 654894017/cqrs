package com.damon.cqrs.rocketmq;

import com.alibaba.fastjson.JSONObject;
import com.damon.cqrs.event.EventSendingContext;
import com.damon.cqrs.event.IEventSendService;
import com.damon.cqrs.exception.EventSendException;
import lombok.extern.slf4j.Slf4j;
import org.apache.rocketmq.client.impl.producer.TopicPublishInfo;
import org.apache.rocketmq.client.producer.SendResult;
import org.apache.rocketmq.client.producer.SendStatus;
import org.apache.rocketmq.common.message.Message;
import org.apache.rocketmq.common.message.MessageQueue;

import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * @author xianping_lu
 */
@Slf4j
public class RocketMQEventSendService implements IEventSendService {

    private final DefaultMQProducer producer;

    private final String topic;

    private final long timeout;

    public RocketMQEventSendService(DefaultMQProducer producer, String topic, long timeout) {
        this.producer = producer;
        this.topic = topic;
        this.timeout = timeout;
    }

    @Override
    public void sendMessage(List<EventSendingContext> eventSendingContexts) {

        TopicPublishInfo topicPublishInfo = producer.tryToFindTopicPublishInfo(topic);
        List<MessageQueue> queues = topicPublishInfo.getMessageQueueList();
        Map<Integer, List<EventSendingContext>> map = eventSendingContexts.stream().collect(Collectors.groupingBy(context -> {
            int hashCode = context.getAggregateId().hashCode();
            hashCode = hashCode < 0 ? Math.abs(hashCode) : hashCode;
            return hashCode % queues.size();
        }));

        map.keySet().parallelStream().forEach((index) -> {
            List<EventSendingContext> contexts = map.get(index);
            List<Message> msgs = contexts.stream().map(event ->
                    new Message(topic, JSONObject.toJSONString(event.getEvents()).getBytes(StandardCharsets.UTF_8))).collect(Collectors.toList()
            );
            MessageQueue queue = queues.get(index);
            SendResult result;
            try {
                result = producer.send(msgs, queue, timeout);
            } catch (Exception e) {
                log.error("event sending failed.", e);
                return;
            }
            if (result.getSendStatus().equals(SendStatus.SEND_OK)) {
                log.info("batch sending event size :{}  succeed.", msgs.size());
            } else {
                throw new EventSendException("rocketmq event store failed, status : " + result.getSendStatus());
            }

        });
    }
}
