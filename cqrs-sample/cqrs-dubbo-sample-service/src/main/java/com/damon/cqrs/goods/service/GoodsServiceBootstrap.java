package com.damon.cqrs.goods.service;

import com.damon.cqrs.*;
import com.damon.cqrs.event_store.MysqlEventOffset;
import com.damon.cqrs.event_store.MysqlEventStore;
import com.damon.cqrs.mq.DefaultMQProducer;
import com.damon.cqrs.mq.RocketMQSendSyncService;
import com.zaxxer.hikari.HikariDataSource;
import org.apache.rocketmq.client.exception.MQClientException;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.EnableAutoConfiguration;
import org.springframework.context.annotation.Bean;

import javax.sql.DataSource;

@EnableAutoConfiguration
public class GoodsServiceBootstrap {

    public static void main(String[] args) {
        SpringApplication.run(GoodsServiceBootstrap.class, args);
    }

    @Bean
    public DataSource dataSource() {
        HikariDataSource dataSource = new HikariDataSource();
        dataSource.setJdbcUrl("jdbc:mysql://127.0.0.1:3306/cqrs?serverTimezone=UTC&rewriteBatchedStatements=true");
        dataSource.setUsername("root");
        dataSource.setPassword("root");
        dataSource.setMaximumPoolSize(5);
        dataSource.setMinimumIdle(5);
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

    @Bean
    public DefaultEventSendingShceduler eventSendingShceduler(@Autowired IEventStore store, @Autowired IEventOffset offset) throws MQClientException {
        DefaultMQProducer producer = new DefaultMQProducer();
        producer.setNamesrvAddr("localhost:9876");
        producer.setProducerGroup("test");
        producer.start();
        RocketMQSendSyncService rocketmqService = new RocketMQSendSyncService(producer, "goods_event", 5);
        EventSendingService sendingService = new EventSendingService(rocketmqService, 50, 1024);
        return new DefaultEventSendingShceduler(store, offset, sendingService, 5, 5);
    }

    @Bean
    public EventCommittingService eventCommittingService(@Autowired DataSource dataSource, @Autowired IEventStore store) {
        IAggregateSnapshootService snapshootService = new DefaultAggregateSnapshootService(2, 5);
        IAggregateCache aggregateCache = new DefaultAggregateGuavaCache(1024 * 1024, 30);
        return new EventCommittingService(store, snapshootService, aggregateCache, 1024, 1024);
    }

}