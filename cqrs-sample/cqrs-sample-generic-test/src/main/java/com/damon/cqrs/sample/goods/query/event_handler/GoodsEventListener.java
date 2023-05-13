package com.damon.cqrs.sample.goods.query.event_handler;

import com.alibaba.fastjson.JSONObject;
import com.damon.cqrs.domain.Event;
import com.damon.cqrs.kafka.KafkaEventListener;
import lombok.extern.slf4j.Slf4j;

import java.util.List;
import java.util.Map;

/**
 * goods事件监听器
 *
 * @author xianpinglu
 */
@Slf4j
public class GoodsEventListener extends KafkaEventListener {


    public GoodsEventListener(String topic, String groupId, String bootstrapServers) {
        super(topic, groupId, bootstrapServers);
    }

    @Override
    public void process(Map<Integer, List<List<Event>>> aggregateEventGroup) {
        System.out.println(Thread.currentThread().getName());
        aggregateEventGroup.forEach((aggregateId, events) -> {
            //log.info("aggregate type : {}, event list size: {}.", aggregateId, events.size());
            events.forEach(event -> {
                log.info(JSONObject.toJSONString(event));
            });
        });
    }
}
