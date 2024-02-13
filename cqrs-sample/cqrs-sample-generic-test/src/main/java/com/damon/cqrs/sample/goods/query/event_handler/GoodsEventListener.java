package com.damon.cqrs.sample.goods.query.event_handler;

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


    public GoodsEventListener(String topic, String groupId, int threadNumber, String bootstrapServers) {
        super(topic, groupId, threadNumber, bootstrapServers);
    }

    @Override
    public void process(Map<Integer, List<List<Event>>> aggregateEventGroup) {
        System.out.println(aggregateEventGroup);
    }
}
