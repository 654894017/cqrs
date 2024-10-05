package com.damon.cqrs.kafka;

import com.damon.cqrs.domain.Event;
import com.damon.cqrs.event.IEventListener;
import com.damon.cqrs.kafka.config.KafkaConsumerConfig;
import lombok.extern.slf4j.Slf4j;

import java.util.List;

/**
 * kafka消费者顺序消费
 */
@Slf4j
public abstract class KafkaEventOrderlyListener implements IEventListener {
    public KafkaEventOrderlyListener(KafkaConsumerConfig config) {
        new Thread(() ->
                new KafkaEventDispatch(config.consumerProperties(), config.getTopic(), this::process).start()
        ).start();
    }

    @Override
    public abstract void process(List<List<Event>> events);
}