package com.damon.cqrs.sample.metting.query;

import com.alibaba.fastjson.JSONObject;
import com.damon.cqrs.domain.Event;
import com.damon.cqrs.rocketmq.RocketMQOrderlyEventListener;
import lombok.extern.slf4j.Slf4j;
import org.apache.rocketmq.client.exception.MQClientException;
import org.apache.rocketmq.common.consumer.ConsumeFromWhere;

import java.util.List;
import java.util.Map;

@Slf4j
public class MettingEventHandler extends RocketMQOrderlyEventListener {

    public MettingEventHandler(String nameServer, String topic, String consumerGroup, int minThread, int maxThread, int pullBatchSize, ConsumeFromWhere where) throws MQClientException {
        super(nameServer, topic, consumerGroup, minThread, maxThread, pullBatchSize, where);
    }

    @Override
    public void process(Map<Integer, List<List<Event>>> aggregateEventGroup) {
        aggregateEventGroup.forEach((aggregateType, events) -> {
            log.info("aggregate type : {}, event list size: {}.", aggregateType, events.size());
            events.forEach(event -> {
                log.info(JSONObject.toJSONString(event));
            });
        });
    }
}
