package com.damon.cqrs.goods.service;

import com.damon.cqrs.cache.DefaultAggregateCaffeineCache;
import com.damon.cqrs.cache.IAggregateCache;
import com.damon.cqrs.config.AggregateSlotLock;
import com.damon.cqrs.config.CqrsConfig;
import com.damon.cqrs.event.EventCommittingService;
import com.damon.cqrs.event.ISendMessageService;
import com.damon.cqrs.event_store.DataSourceMapping;
import com.damon.cqrs.event_store.DefaultEventShardingRouting;
import com.damon.cqrs.event_store.MysqlEventOffset;
import com.damon.cqrs.event_store.MysqlEventStore;
import com.damon.cqrs.kafka.KafkaSendService;
import com.damon.cqrs.recovery.AggregateRecoveryService;
import com.damon.cqrs.rocketmq.DefaultMQProducer;
import com.damon.cqrs.rocketmq.RocketMQSendService;
import com.damon.cqrs.snapshot.DefaultAggregateSnapshootService;
import com.damon.cqrs.snapshot.IAggregateSnapshootService;
import com.damon.cqrs.store.IEventOffset;
import com.damon.cqrs.store.IEventStore;
import com.google.common.collect.Lists;
import com.zaxxer.hikari.HikariDataSource;

import java.util.List;

public class TestConfig {

    public static HikariDataSource dataSource() {
        HikariDataSource dataSource = new HikariDataSource();
        dataSource.setJdbcUrl("jdbc:mysql://localhost:3307/cqrs?serverTimezone=UTC&rewriteBatchedStatements=true");
        dataSource.setUsername("root");
        dataSource.setPassword("root");
        dataSource.setMaximumPoolSize(20);
        dataSource.setMinimumIdle(20);
        dataSource.setDriverClassName(com.mysql.cj.jdbc.Driver.class.getTypeName());
        return dataSource;
    }

    public static HikariDataSource dataSource2() {
        HikariDataSource dataSource = new HikariDataSource();
        dataSource.setJdbcUrl("jdbc:mysql://localhost:3307/cqrs2?serverTimezone=UTC&rewriteBatchedStatements=true");
        dataSource.setUsername("root");
        dataSource.setPassword("root");
        dataSource.setMaximumPoolSize(20);
        dataSource.setMinimumIdle(20);
        dataSource.setDriverClassName(com.mysql.cj.jdbc.Driver.class.getTypeName());
        return dataSource;
    }

    public static CqrsConfig init() {
        List<DataSourceMapping> list = Lists.newArrayList(
                DataSourceMapping.builder().dataSourceName("ds0").dataSource(dataSource()).tableNumber(4).build(),
                DataSourceMapping.builder().dataSourceName("ds1").dataSource(dataSource2()).tableNumber(4).build()
        );
        DefaultEventShardingRouting route = new DefaultEventShardingRouting();
        IEventStore store = new MysqlEventStore(list, 32, route);
        IEventOffset offset = new MysqlEventOffset(list);
        IAggregateSnapshootService aggregateSnapshootService = new DefaultAggregateSnapshootService(8, 6);
        IAggregateCache aggregateCache = new DefaultAggregateCaffeineCache(1024 * 1024, 60);
        DefaultMQProducer producer = new DefaultMQProducer();
        producer.setNamesrvAddr("localhost:9876");
        producer.setProducerGroup("test");
        //producer.start();
        RocketMQSendService rocketmqService = new RocketMQSendService(producer, "event_queue", 5);
        ISendMessageService sendingService = new KafkaSendService("event_queue", "10.230.5.244:9092,10.230.4.87:9092,10.230.5.152:9092");
        //new DefaultEventSendingShceduler(store, offset, sendingService,  5);
        AggregateSlotLock aggregateSlotLock = new AggregateSlotLock(4096);
        AggregateRecoveryService aggregateRecoveryService = new AggregateRecoveryService(store, aggregateCache, aggregateSlotLock);
        EventCommittingService eventCommittingService = new EventCommittingService(store, 16, 1024 * 4, 32, aggregateRecoveryService);

        CqrsConfig cqrsConfig = CqrsConfig.builder().
                eventStore(store).aggregateSnapshootService(aggregateSnapshootService).aggregateCache(aggregateCache).
                aggregateSlotLock(aggregateSlotLock).
                eventCommittingService(eventCommittingService).build();
        return cqrsConfig;
    }


}
