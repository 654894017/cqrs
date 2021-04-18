package com.nd.cqrs.mq;

import java.util.concurrent.ConcurrentMap;

import org.apache.rocketmq.client.impl.MQClientManager;
import org.apache.rocketmq.client.impl.factory.MQClientInstance;
import org.apache.rocketmq.client.impl.producer.DefaultMQProducerImpl;
import org.apache.rocketmq.client.impl.producer.TopicPublishInfo;
import org.apache.rocketmq.remoting.RPCHook;

public class DefaultMQProducer extends org.apache.rocketmq.client.producer.DefaultMQProducer {

    public TopicPublishInfo tryToFindTopicPublishInfo(String topic) {
        DefaultMQProducerImpl defaultMQProducerImpl = super.defaultMQProducerImpl;
        ConcurrentMap<String, TopicPublishInfo> topicPublishInfoMap = defaultMQProducerImpl.getTopicPublishInfoTable();
        TopicPublishInfo topicPublishInfo = topicPublishInfoMap.get(topic);
        MQClientInstance mQClientFactory = MQClientManager.getInstance().getOrCreateMQClientInstance(this, null);
        if (null == topicPublishInfo || !topicPublishInfo.ok()) {
            topicPublishInfoMap.putIfAbsent(topic, new TopicPublishInfo());
            mQClientFactory.updateTopicRouteInfoFromNameServer(topic);
            topicPublishInfo = topicPublishInfoMap.get(topic);
        }

        if (!topicPublishInfo.isHaveTopicRouterInfo() && !topicPublishInfo.ok()) {
            mQClientFactory.updateTopicRouteInfoFromNameServer(topic, true, this);
            topicPublishInfo = topicPublishInfoMap.get(topic);
            return topicPublishInfo;
        } else {
            return topicPublishInfo;
        }

    }

    @SuppressWarnings("unused")
    private RPCHook rpcHook;

    public DefaultMQProducer() {
        super();
    }

    public DefaultMQProducer(RPCHook rpcHook) {
        super(rpcHook);
        this.rpcHook = rpcHook;
    }

    public DefaultMQProducer(String producerGroup) {
        super(producerGroup);
    }

    public DefaultMQProducer(String producerGroup, RPCHook rpcHook, boolean enableMsgTrace, String customizedTraceTopic, RPCHook rpcHook1) {
        super(producerGroup, rpcHook, enableMsgTrace, customizedTraceTopic);
        this.rpcHook = rpcHook1;
    }

    public DefaultMQProducer(String namespace, String producerGroup, RPCHook rpcHook) {
        super(namespace, producerGroup);
        this.rpcHook = rpcHook;
    }

    public DefaultMQProducer(String producerGroup, RPCHook rpcHook) {
        super(producerGroup, rpcHook);
        this.rpcHook = rpcHook;
    }


    public DefaultMQProducer(String producerGroup, boolean enableMsgTrace, RPCHook rpcHook) {
        super(producerGroup, enableMsgTrace);
        this.rpcHook = rpcHook;
    }

    public DefaultMQProducer(String producerGroup, boolean enableMsgTrace, String customizedTraceTopic, RPCHook rpcHook) {
        super(producerGroup, enableMsgTrace, customizedTraceTopic);
        this.rpcHook = rpcHook;
    }

    public DefaultMQProducer(String namespace, String producerGroup, RPCHook rpcHook, boolean enableMsgTrace, String customizedTraceTopic) {
        super(namespace, producerGroup, rpcHook, enableMsgTrace, customizedTraceTopic);
        this.rpcHook = rpcHook;
    }


}
