package com.damon.cqrs.goods.service;

import com.damon.cqrs.DefaultAggregateGuavaCache;
import com.damon.cqrs.DefaultAggregateSnapshootService;
import com.damon.cqrs.IAggregateCache;
import com.damon.cqrs.IAggregateSnapshootService;
import com.damon.cqrs.event.DefaultEventSendingShceduler;
import com.damon.cqrs.event.EventCommittingService;
import com.damon.cqrs.event.EventSendingService;
import com.damon.cqrs.event_store.MysqlEventOffset;
import com.damon.cqrs.event_store.MysqlEventStore;
import com.damon.cqrs.rocketmq.RocketMQSendSyncService;
import com.damon.cqrs.rocketmq.DefaultMQProducer;
import com.damon.cqrs.store.IEventOffset;
import com.damon.cqrs.store.IEventStore;
import com.zaxxer.hikari.HikariDataSource;
import org.apache.rocketmq.client.exception.MQClientException;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import javax.sql.DataSource;

@Configuration
public class CqrsConfig {

    private final String CQRS_EVENT_QUEUE = "cqrs_event_queue";

//    @Bean
//    public GoodsEventListener listener() throws MQClientException {
//        return new GoodsEventListener(
//                "localhost:9876",
//                CQRS_EVENT_QUEUE,
//                "test_group",
//                50,
//                50,
//                1024
//        );
//    }

    @Bean
    public DataSource dataSource() {
        HikariDataSource dataSource = new HikariDataSource();
        dataSource.setJdbcUrl("jdbc:mysql://127.0.0.1:3306/cqrs?serverTimezone=UTC&rewriteBatchedStatements=true");
        dataSource.setUsername("root");
        dataSource.setPassword("root");
        dataSource.setMaximumPoolSize(128);
        dataSource.setMinimumIdle(128);
        dataSource.setDriverClassName(com.mysql.cj.jdbc.Driver.class.getTypeName());
        return dataSource;
    }

    @Bean
    public IEventStore eventStore(@Autowired DataSource dataSource) {
        return new MysqlEventStore(dataSource);
    }

    @Bean
    public IEventOffset eventOffset(@Autowired DataSource dataSource) {
        return new MysqlEventOffset(dataSource);
    }

   // @Bean
    public DefaultEventSendingShceduler eventSendingShceduler(@Autowired IEventStore store, @Autowired IEventOffset offset) throws MQClientException {
        DefaultMQProducer producer = new DefaultMQProducer();
        producer.setNamesrvAddr("localhost:9876");
        producer.setProducerGroup("test");
        producer.setVipChannelEnabled(false);
        producer.start();
        RocketMQSendSyncService rocketmqService = new RocketMQSendSyncService(producer, CQRS_EVENT_QUEUE, 5000L);
        EventSendingService sendingService = new EventSendingService(rocketmqService, 50, 1024);
        return new DefaultEventSendingShceduler(store, offset, sendingService, 5, 5);
    }

    @Bean
    public EventCommittingService eventCommittingService(@Autowired DataSource dataSource, @Autowired IEventStore store) {
        IAggregateSnapshootService snapshootService = new DefaultAggregateSnapshootService(2, 5);
        IAggregateCache aggregateCache = new DefaultAggregateGuavaCache(1024 * 1024, 30);
        return new EventCommittingService(store, snapshootService, aggregateCache, 4, 1024);
    }
}
