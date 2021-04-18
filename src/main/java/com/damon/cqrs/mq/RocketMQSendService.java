package com.damon.cqrs.mq;

import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.stream.Collectors;

import org.apache.rocketmq.client.exception.MQBrokerException;
import org.apache.rocketmq.client.exception.MQClientException;
import org.apache.rocketmq.client.impl.producer.TopicPublishInfo;
import org.apache.rocketmq.client.producer.SendCallback;
import org.apache.rocketmq.client.producer.SendResult;
import org.apache.rocketmq.client.producer.SendStatus;
import org.apache.rocketmq.common.message.Message;
import org.apache.rocketmq.common.message.MessageQueue;
import org.apache.rocketmq.remoting.exception.RemotingException;

import com.alibaba.fastjson.JSONObject;
import com.damon.cqrs.EventSendingContext;
import com.damon.cqrs.ISendMessageService;
import com.damon.cqrs.exception.EventSendingException;

import lombok.extern.slf4j.Slf4j;

/**
 * 
 * @author xianping_lu
 *
 */
@Slf4j
public class RocketMQSendService implements ISendMessageService {

    private final DefaultMQProducer producer;

    private String topic;

    private int timeout = 5000;

    public RocketMQSendService(DefaultMQProducer producer, String topic) {
        this.producer = producer;
        this.topic = topic;
    }

    @Override
    public void sendMessage(List<EventSendingContext> eventSendingContexts) {
        CompletableFuture<Boolean> future = new CompletableFuture<>();
        Map<Long, List<EventSendingContext>> map = eventSendingContexts.stream().collect(Collectors.groupingBy(EventSendingContext::getAggregateId));
        TopicPublishInfo topicPublishInfo = producer.tryToFindTopicPublishInfo(topic);
        List<MessageQueue> queues = topicPublishInfo.getMessageQueueList();
        map.keySet().parallelStream().forEach((aggregateId) -> {
            List<EventSendingContext> contexts = map.get(aggregateId);
            List<Message> msgs = contexts.stream().map(event -> {
                return new Message(topic, JSONObject.toJSONString(event.getEvents()).getBytes(StandardCharsets.UTF_8));
            }).collect(Collectors.toList());
            int index = Math.abs(aggregateId.hashCode()) % queues.size();
            MessageQueue queue = queues.get(index);
            try {
                producer.send(msgs, queue, new SendCallback() {
                    @Override
                    public void onSuccess(SendResult result) {
                        if (result.getSendStatus().equals(SendStatus.SEND_OK)) {
                            log.info("aggregate id : {}, aggregate type: {}, batch send event size :{} to topic:{} success.", aggregateId, contexts.get(0).getAggregateType(), msgs.size(), topic);
                            contexts.forEach(context -> context.getFuture().complete(true));
                        } else {
                            contexts.forEach(context -> {
                                context.getFuture().completeExceptionally(new EventSendingException("message send fail status : " + result.getSendStatus()));
                            });
                        }
                    }

                    @Override
                    public void onException(Throwable e) {
                        future.completeExceptionally(new EventSendingException(e));
                    }
                }, timeout);
            } catch (MQClientException | RemotingException | MQBrokerException | InterruptedException e) {
                eventSendingContexts.forEach(context -> context.getFuture().completeExceptionally(new EventSendingException(e)));
            }
        });
    }

}
