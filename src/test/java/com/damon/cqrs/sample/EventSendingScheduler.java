package com.damon.cqrs.sample;

import java.util.List;
import java.util.concurrent.CompletableFuture;

import org.apache.rocketmq.client.exception.MQClientException;
import org.springframework.jdbc.core.JdbcTemplate;

import com.damon.cqrs.EventSendingContext;
import com.damon.cqrs.EventSendingService;
import com.damon.cqrs.IEventStore;
import com.damon.cqrs.mq.DefaultMQProducer;
import com.damon.cqrs.mq.RocketMQSendService;
import com.damon.cqrs.store.MysqlEventStore;
import com.zaxxer.hikari.HikariDataSource;

public class EventSendingScheduler {

    public static void main(String[] args) throws MQClientException {
        DefaultMQProducer producer = new DefaultMQProducer();
        producer.setNamesrvAddr("localhost:9876");
        producer.setProducerGroup("test");
        producer.start();
        RocketMQSendService rocketmqService = new RocketMQSendService(producer, "TTTTTT");

        EventSendingService eventSendingService = new EventSendingService(rocketmqService, 10, 1024);
        IEventStore store = new MysqlEventStore(new JdbcTemplate(dataSource()));
        CompletableFuture<List<EventSendingContext>> future = store.queryWaitingSend(1);
        List<EventSendingContext> context = future.join();
        context.forEach(c -> eventSendingService.sendDomainEventAsync(c));

    }

    public static HikariDataSource dataSource() {
        HikariDataSource dataSource = new HikariDataSource();
        dataSource.setJdbcUrl("jdbc:mysql://127.0.0.1:3306/enode?serverTimezone=UTC&rewriteBatchedStatements=true");
        dataSource.setUsername("root");
        dataSource.setPassword("root");
        dataSource.setMaximumPoolSize(10);
        dataSource.setMinimumIdle(10);
        dataSource.setDriverClassName(com.mysql.cj.jdbc.Driver.class.getName());
        return dataSource;
    }
}
