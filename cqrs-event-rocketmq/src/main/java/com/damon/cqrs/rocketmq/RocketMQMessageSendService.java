package com.damon.cqrs.rocketmq;

import com.alibaba.fastjson.JSONObject;
import com.damon.cqrs.domain.Command;
import com.damon.cqrs.rocketmq.core.IMessageSendService;
import lombok.extern.slf4j.Slf4j;
import org.apache.rocketmq.client.exception.MQClientException;
import org.apache.rocketmq.client.producer.DefaultMQProducer;
import org.apache.rocketmq.client.producer.MessageQueueSelector;
import org.apache.rocketmq.client.producer.SendCallback;
import org.apache.rocketmq.client.producer.SendResult;
import org.apache.rocketmq.common.message.Message;
import org.apache.rocketmq.common.message.MessageQueue;
import org.apache.rocketmq.remoting.exception.RemotingException;

import java.util.List;
import java.util.concurrent.CompletableFuture;

@Slf4j
public class RocketMQMessageSendService implements IMessageSendService {

    private final DefaultMQProducer producer;

    public RocketMQMessageSendService(DefaultMQProducer producer) {
        this.producer = producer;
    }

    @Override
    public CompletableFuture<Boolean> sendMessage(Command command) {
        Message message = new Message("cqrs-command-queue", JSONObject.toJSONString(command).getBytes());
        // String commandType = command.getClass().getName();
        CompletableFuture<Boolean> future = new CompletableFuture<>();
        try {
            producer.fetchPublishMessageQueues("");
            producer.send(message, new MessageQueueSelector() {
                @Override
                public MessageQueue select(List<MessageQueue> mqs, Message msg, Object aid) {
                    Long aggregateId = (Long) aid;
                    int hash = Long.hashCode(aggregateId);
                    if (hash < 0) {
                        hash = Math.abs(hash);
                    }
                    int size = mqs.size();
                    int index = hash % size;
                    return mqs.get(index);
                }
            }, command.getAggregateId(), new SendCallback() {
                @Override
                public void onSuccess(SendResult sendResult) {
                    future.complete(true);
                    log.debug("send message to rocketmq succeed, message: {} ", JSONObject.toJSONString(message));
                }

                @Override
                public void onException(Throwable e) {
                    future.completeExceptionally(e);
                    log.error("send message to rocketmq failed, message: {} ", JSONObject.toJSONString(message));
                }
            });
        } catch (MQClientException | RemotingException | InterruptedException e) {
            future.completeExceptionally(e);
            throw new RuntimeException("send message to rocketmq failed ", e);
        }
        return future;
    }

}
