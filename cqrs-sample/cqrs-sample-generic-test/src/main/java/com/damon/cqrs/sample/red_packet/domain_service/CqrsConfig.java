package com.damon.cqrs.sample.red_packet.domain_service;

import com.damon.cqrs.DefaultAggregateGuavaCache;
import com.damon.cqrs.DefaultAggregateSnapshootService;
import com.damon.cqrs.IAggregateCache;
import com.damon.cqrs.IAggregateSnapshootService;
import com.damon.cqrs.event.EventCommittingService;
import com.damon.cqrs.event.EventSendingService;
import com.damon.cqrs.event_store.MysqlEventOffset;
import com.damon.cqrs.event_store.MysqlEventStore;
import com.damon.cqrs.rocketmq.RocketMQSendSyncService;
import com.damon.cqrs.rocketmq.core.DefaultMQProducer;
import com.damon.cqrs.store.IEventOffset;
import com.damon.cqrs.store.IEventStore;
import com.zaxxer.hikari.HikariDataSource;
import org.apache.rocketmq.client.exception.MQClientException;

public class CqrsConfig {

    private static HikariDataSource dataSource() {
        HikariDataSource dataSource = new HikariDataSource();
        dataSource.setJdbcUrl("jdbc:mysql://localhost:3306/cqrs?serverTimezone=UTC&rewriteBatchedStatements=true");
        dataSource.setUsername("root");
        dataSource.setPassword("root");
        dataSource.setMaximumPoolSize(5);
        dataSource.setMinimumIdle(5);
        dataSource.setDriverClassName(com.mysql.cj.jdbc.Driver.class.getTypeName());
        return dataSource;
    }

    public static EventCommittingService init() throws MQClientException {
        IEventStore store = new MysqlEventStore(dataSource());
        IEventOffset offset = new MysqlEventOffset(dataSource());
        IAggregateSnapshootService aggregateSnapshootService = new DefaultAggregateSnapshootService(10, 5);
        IAggregateCache aggregateCache = new DefaultAggregateGuavaCache(1024 * 1024, 30);
        DefaultMQProducer producer = new DefaultMQProducer();
        producer.setNamesrvAddr("localhost:9876");
        producer.setProducerGroup("test");
        //producer.start();
        RocketMQSendSyncService rocketmqService = new RocketMQSendSyncService(producer, "cqrs_event_queue", 5);
        EventSendingService sendingService = new EventSendingService(rocketmqService, 32, 1024);
        // new DefaultEventSendingShceduler(store, offset, sendingService, 5, 5);
        return new EventCommittingService(store, aggregateSnapshootService, aggregateCache, 128, 2048);

    }


}
