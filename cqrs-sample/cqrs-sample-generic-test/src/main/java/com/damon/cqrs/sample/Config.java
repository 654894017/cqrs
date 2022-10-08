package com.damon.cqrs.sample;

import com.damon.cqrs.*;
import com.damon.cqrs.event.EventCommittingService;
import com.damon.cqrs.event.EventSendingService;
import com.damon.cqrs.event_store.DataSourceMapping;
import com.damon.cqrs.event_store.DefaultEventShardingRouting;
import com.damon.cqrs.event_store.MysqlEventOffset;
import com.damon.cqrs.event_store.MysqlEventStore;
import com.damon.cqrs.rocketmq.DefaultMQProducer;
import com.damon.cqrs.rocketmq.RocketMQSendSyncService;
import com.damon.cqrs.store.IEventOffset;
import com.damon.cqrs.store.IEventStore;
import com.google.common.collect.Lists;
import com.zaxxer.hikari.HikariDataSource;
import org.apache.rocketmq.client.exception.MQClientException;

import java.util.List;

public class Config {

    public static HikariDataSource dataSource() {
        HikariDataSource dataSource = new HikariDataSource();
        dataSource.setJdbcUrl("jdbc:mysql://localhost:3306/cqrs?serverTimezone=UTC&rewriteBatchedStatements=true");
        dataSource.setUsername("root");
        dataSource.setPassword("01386871");
        dataSource.setMaximumPoolSize(20);
        dataSource.setMinimumIdle(20);
        dataSource.setDriverClassName(com.mysql.cj.jdbc.Driver.class.getTypeName());
        return dataSource;
    }

    public static HikariDataSource dataSource2() {
        HikariDataSource dataSource = new HikariDataSource();
        dataSource.setJdbcUrl("jdbc:mysql://localhost:3306/cqrs2?serverTimezone=UTC&rewriteBatchedStatements=true");
        dataSource.setUsername("root");
        dataSource.setPassword("01386871");
        dataSource.setMaximumPoolSize(20);
        dataSource.setMinimumIdle(20);
        dataSource.setDriverClassName(com.mysql.cj.jdbc.Driver.class.getTypeName());
        return dataSource;
    }

    public static CQRSConfig init() throws MQClientException {
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
        RocketMQSendSyncService rocketmqService = new RocketMQSendSyncService(producer, "event_queue", 5);
        EventSendingService sendingService = new EventSendingService(rocketmqService, 32, 1024);
        //new DefaultEventSendingShceduler(store, offset, sendingService,  5);
        IBeanCopy beanCopy = new DefaultCglibBeanCopy();
        AggregateRecoveryService aggregateRecoveryService = new AggregateRecoveryService(store, aggregateCache);
        EventCommittingService eventCommittingService = new EventCommittingService(store, 16, 1024*4, 16, 32, aggregateRecoveryService);

        CQRSConfig config = CQRSConfig.builder().//.beanCopy(beanCopy).
                eventStore(store).aggregateSnapshootService(aggregateSnapshootService).aggregateCache(aggregateCache).
                eventCommittingService(eventCommittingService).build();
        return config;
    }


}
