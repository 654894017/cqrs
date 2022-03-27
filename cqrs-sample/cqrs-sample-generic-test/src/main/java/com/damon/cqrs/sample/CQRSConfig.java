package com.damon.cqrs.sample;

import com.alibaba.druid.pool.DruidDataSource;
import com.damon.cqrs.DefaultAggregateGuavaCache;
import com.damon.cqrs.DefaultAggregateSnapshootService;
import com.damon.cqrs.IAggregateCache;
import com.damon.cqrs.IAggregateSnapshootService;
import com.damon.cqrs.event.EventCommittingService;
import com.damon.cqrs.event.EventSendingService;
import com.damon.cqrs.event_store.DataSourceMapping;
import com.damon.cqrs.event_store.MysqlEventOffset;
import com.damon.cqrs.event_store.MysqlEventStore;
import com.damon.cqrs.rocketmq.DefaultMQProducer;
import com.damon.cqrs.rocketmq.RocketMQSendSyncService;
import com.damon.cqrs.store.IEventOffset;
import com.damon.cqrs.store.IEventStore;
import com.google.common.collect.Lists;
import com.zaxxer.hikari.HikariDataSource;
import org.apache.rocketmq.client.exception.MQClientException;

import javax.sql.DataSource;
import java.util.List;

public class CQRSConfig {

    private static DataSource dataSource2() {
        DruidDataSource dataSource = new DruidDataSource();
        dataSource.setUrl("jdbc:mysql://localhost:3306/cqrs2?serverTimezone=UTC&rewriteBatchedStatements=true");
        dataSource.setUsername("root");
        dataSource.setPassword("root");
        dataSource.setMaxActive(40);
        dataSource.setMinIdle(40);
        dataSource.setDriverClassName(com.mysql.cj.jdbc.Driver.class.getTypeName());
        return dataSource;
    }


    private static DataSource dataSource() {
        DruidDataSource dataSource = new DruidDataSource();
        dataSource.setUrl("jdbc:mysql://localhost:3306/cqrs?serverTimezone=UTC&rewriteBatchedStatements=true");
        dataSource.setUsername("root");
        dataSource.setPassword("root");
        dataSource.setMaxActive(40);
        dataSource.setMinIdle(40);
        dataSource.setDriverClassName(com.mysql.cj.jdbc.Driver.class.getTypeName());
        return dataSource;
    }



//    private static HikariDataSource dataSource() {
//        HikariDataSource dataSource = new HikariDataSource();
//        dataSource.setJdbcUrl("jdbc:mysql://localhost:3306/cqrs?serverTimezone=UTC&rewriteBatchedStatements=true");
//        dataSource.setUsername("root");
//        dataSource.setPassword("root");
//        dataSource.setMaximumPoolSize(40);
//        dataSource.setMinimumIdle(40);
//        dataSource.setDriverClassName(com.mysql.cj.jdbc.Driver.class.getTypeName());
//        return dataSource;
//    }
//
//    private static HikariDataSource dataSource2() {
//        HikariDataSource dataSource = new HikariDataSource();
//        dataSource.setJdbcUrl("jdbc:mysql://localhost:3306/cqrs2?serverTimezone=UTC&rewriteBatchedStatements=true");
//        dataSource.setUsername("root");
//        dataSource.setPassword("root");
//        dataSource.setMaximumPoolSize(40);
//        dataSource.setMinimumIdle(40);
//        dataSource.setDriverClassName(com.mysql.cj.jdbc.Driver.class.getTypeName());
//        return dataSource;
//    }

    public static EventCommittingService init() throws MQClientException {
        List<DataSourceMapping> list = Lists.newArrayList(
                DataSourceMapping.builder().dataSourceName("ds0").dataSource(dataSource()).tableNumber(2).build(),
                DataSourceMapping.builder().dataSourceName("ds1").dataSource(dataSource2()).tableNumber(2).build()
        );
        IEventStore store = new MysqlEventStore(list, 32);
        IEventOffset offset = new MysqlEventOffset(list);
        IAggregateSnapshootService aggregateSnapshootService = new DefaultAggregateSnapshootService(1, 3600);
        IAggregateCache aggregateCache = new DefaultAggregateGuavaCache(1024 * 1024, 60);
        DefaultMQProducer producer = new DefaultMQProducer();
        producer.setNamesrvAddr("localhost:9876");
        producer.setProducerGroup("test");
        //producer.start();
        RocketMQSendSyncService rocketmqService = new RocketMQSendSyncService(producer, "red_packet_event_queue", 5);
        EventSendingService sendingService = new EventSendingService(rocketmqService, 32, 1024);
        //new DefaultEventSendingShceduler(store, offset, sendingService,  5);
        return new EventCommittingService(store, aggregateSnapshootService, aggregateCache, 32, 2048, 16);

    }


}
