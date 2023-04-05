package com.damon.cqrs.goods.service;

import com.alibaba.fastjson.JSONObject;
import com.damon.cqrs.domain.Event;
import com.damon.cqrs.rocketmq.RocketMQOrderlyEventListener;
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
public class GoodsEventListener extends RocketMQOrderlyEventListener {

    public GoodsEventListener(String nameServer, String topic, String consumerGroup, int minThread, int maxThread, int pullBatchSize) throws MQClientException {
        super(nameServer, topic, consumerGroup, minThread, maxThread, pullBatchSize);
    }

    @Override
    public void process(Map<String, List<List<Event>>> aggregateEventGroup) {
        aggregateEventGroup.forEach((aggregateId, events) -> {
            log.info("aggregate type : {}, event list size: {}.", aggregateId, events.size());
            events.forEach(event -> {
                log.info(JSONObject.toJSONString(event));
            });
        });
    }
}
