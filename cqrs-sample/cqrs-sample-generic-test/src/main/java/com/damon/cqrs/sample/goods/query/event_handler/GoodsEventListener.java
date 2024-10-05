package com.damon.cqrs.sample.goods.query.event_handler;

import com.alibaba.fastjson.JSONObject;
import com.damon.cqrs.domain.Event;
import com.damon.cqrs.kafka.KafkaEventOrderlyListener;
import com.damon.cqrs.kafka.config.KafkaConsumerConfig;
import lombok.extern.slf4j.Slf4j;

import java.util.List;

/**
 * goods事件监听器
 *
 * @author xianpinglu
 */
@Slf4j
public class GoodsEventListener extends KafkaEventOrderlyListener {

    public GoodsEventListener(KafkaConsumerConfig consumerConfig) {
        super(consumerConfig);
    }

    @Override
    public void process(List<List<Event>> events) {
        events.forEach(eventList -> {
            System.out.println(Thread.currentThread().getName() + ":" + JSONObject.toJSONString(eventList));
        });
    }
}
