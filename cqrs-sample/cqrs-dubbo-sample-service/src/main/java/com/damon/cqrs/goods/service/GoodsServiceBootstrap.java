package com.damon.cqrs.goods.service;

import javax.sql.DataSource;

import org.apache.rocketmq.client.exception.MQClientException;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.EnableAutoConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.jdbc.core.JdbcTemplate;

import com.damon.cqrs.DefaultAggregateCache;
import com.damon.cqrs.DefaultAggregateSnapshootService;
import com.damon.cqrs.DefaultEventSendingShceduler;
import com.damon.cqrs.EventCommittingService;
import com.damon.cqrs.EventSendingService;
import com.damon.cqrs.IAggregateCache;
import com.damon.cqrs.IAggregateSnapshootService;
import com.damon.cqrs.IEventStore;
import com.damon.cqrs.mq.DefaultMQProducer;
import com.damon.cqrs.mq.RocketMQSendSyncService;
import com.damon.cqrs.store.MysqlEventStore;
import com.zaxxer.hikari.HikariDataSource;

@EnableAutoConfiguration
public class GoodsServiceBootstrap {

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
        return new MysqlEventStore(new JdbcTemplate(dataSource));
    }

    @Bean
    public DefaultEventSendingShceduler eventSendingShceduler(@Autowired IEventStore store) throws MQClientException {
        DefaultMQProducer producer = new DefaultMQProducer();
        producer.setNamesrvAddr("localhost:9876");
        producer.setProducerGroup("test");
        producer.start();
        RocketMQSendSyncService rocketmqService = new RocketMQSendSyncService(producer, "goods_event", 5);
        EventSendingService sendingService = new EventSendingService(rocketmqService, 50, 1024);
        return new DefaultEventSendingShceduler(store, sendingService, 5, 5);
    }

    @Bean
    public EventCommittingService eventCommittingService(@Autowired DataSource dataSource, @Autowired IEventStore store) {
        IAggregateSnapshootService snapshootService = new DefaultAggregateSnapshootService(2, 5);
        IAggregateCache aggregateCache = new DefaultAggregateCache(1024 * 1024, 30);
        return new EventCommittingService(store, snapshootService, aggregateCache, 1024, 1024);
    }

    public static void main(String[] args) {
        SpringApplication.run(GoodsServiceBootstrap.class, args);
    }

}