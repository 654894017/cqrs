package com.damon.cqrs.mq;

import com.alibaba.fastjson.JSONObject;
import com.damon.cqrs.EventSendingContext;
import com.damon.cqrs.ISendMessageService;
import com.damon.cqrs.utils.ThreadUtils;
import lombok.extern.slf4j.Slf4j;
import org.apache.rocketmq.client.exception.MQBrokerException;
import org.apache.rocketmq.client.exception.MQClientException;
import org.apache.rocketmq.client.impl.producer.TopicPublishInfo;
import org.apache.rocketmq.client.producer.SendResult;
import org.apache.rocketmq.client.producer.SendStatus;
import org.apache.rocketmq.common.message.Message;
import org.apache.rocketmq.common.message.MessageQueue;
import org.apache.rocketmq.remoting.exception.RemotingException;

import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * @author xianping_lu
 */
@Slf4j
public class RocketMQSendSyncService implements ISendMessageService {

    private final DefaultMQProducer producer;

    private final String topic;

    private final int timeout;

    public RocketMQSendSyncService(DefaultMQProducer producer, String topic, int timeout) {
        this.producer = producer;
        this.topic = topic;
        this.timeout = timeout;
    }

    @Override
    public void sendMessage(List<EventSendingContext> eventSendingContexts) {
        for (; ; ) {
            try {
                Map<Long, List<EventSendingContext>> map = eventSendingContexts.stream().collect(Collectors.groupingBy(EventSendingContext::getAggregateId));
                TopicPublishInfo topicPublishInfo = producer.tryToFindTopicPublishInfo(topic);
                List<MessageQueue> queues = topicPublishInfo.getMessageQueueList();
                map.keySet().parallelStream().forEach((aggregateId) -> {
                    List<EventSendingContext> contexts = map.get(aggregateId);
                    List<Message> msgs = contexts.stream().map(event ->
                            new Message(topic, JSONObject.toJSONString(event.getEvents()).getBytes(StandardCharsets.UTF_8))).collect(Collectors.toList());
                    int index = Math.abs(aggregateId.hashCode()) % queues.size();
                    MessageQueue queue = queues.get(index);
                    for (; ; ) {
                        SendResult result;
                        try {
                            result = producer.send(msgs, queue, timeout);
                        } catch (MQClientException | RemotingException | MQBrokerException | InterruptedException e) {
                            contexts.forEach(context -> log.error("aggregate id : {}, aggregate type: {}, event send failed.", context.getAggregateId(), context.getAggregateType(), e));
                            ThreadUtils.sleep(5000);
                            continue;
                        }
                        if (result.getSendStatus().equals(SendStatus.SEND_OK)) {
                            log.info("aggregate id : {}, aggregate type: {}, batch send event size :{} to topic:{} succeed.", aggregateId, contexts.get(0).getAggregateType(), msgs.size(), topic);
                            contexts.forEach(context -> context.getFuture().complete(true));
                            return;
                        } else {
                            contexts.forEach(context -> log.error("aggregate id : {}, event send failed,  status : {}", context.getAggregateId(), context.getAggregateType(), result.getSendStatus()));
                            ThreadUtils.sleep(5000);
                        }
                    }
                });
            } catch (Throwable e) {
                log.error("event send failture ", e);
                ThreadUtils.sleep(5000);
            }
        }
    }
}
