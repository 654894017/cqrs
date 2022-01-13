package com.damon.cqrs.goods.service;

import com.alibaba.fastjson.JSONObject;
import com.damon.cqrs.domain.Event;
import com.damon.cqrs.rocketmq.RocketMQEventListener;
import lombok.extern.slf4j.Slf4j;
import org.apache.rocketmq.client.exception.MQClientException;

import java.util.List;
import java.util.Map;

/**
 * goods事件监听器
 *
 * @author xianpinglu
 */
@Slf4j
public class GoodsEventListener extends RocketMQEventListener {

    public GoodsEventListener(String nameServer, String topic, String consumerGroup, int minThread, int maxThread, int pullBatchSize) throws MQClientException {
        super(nameServer, topic, consumerGroup, minThread, maxThread, pullBatchSize);
    }

    @Override
    public void process(Map<String, List<Event>> eventGroup) {
        eventGroup.forEach((type, events) -> {
            log.info("event type: {}, size: {}", type, events.size());
            events.forEach(event -> {
                log.info(JSONObject.toJSONString(event));
            });
        });
    }
}
