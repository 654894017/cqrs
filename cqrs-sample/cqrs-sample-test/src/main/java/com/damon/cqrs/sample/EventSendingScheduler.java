package com.damon.cqrs.sample;

import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.stream.Collectors;

import org.apache.rocketmq.client.exception.MQClientException;
import org.springframework.jdbc.core.JdbcTemplate;

import com.damon.cqrs.EventSendingContext;
import com.damon.cqrs.EventSendingService;
import com.damon.cqrs.IEventStore;
import com.damon.cqrs.mq.DefaultMQProducer;
import com.damon.cqrs.mq.RocketMQSendAsyncService;
import com.damon.cqrs.store.MysqlEventStore;
import com.zaxxer.hikari.HikariDataSource;

public class EventSendingScheduler {

    public static void main(String[] args) throws MQClientException {
        DefaultMQProducer producer = new DefaultMQProducer();
        producer.setNamesrvAddr("localhost:9876");
        producer.setProducerGroup("test");
        producer.start();
        RocketMQSendAsyncService rocketmqService = new RocketMQSendAsyncService(producer, "TTTTTT", 5);
        EventSendingService eventSendingService = new EventSendingService(rocketmqService, 10, 1024);
        IEventStore store = new MysqlEventStore(new JdbcTemplate(dataSource()));
        CompletableFuture<List<EventSendingContext>> contextsFuture = store.queryWaitingSendEvents(1);
        List<EventSendingContext> contexts = contextsFuture.join();
        List<CompletableFuture<Boolean>> futures = contexts.parallelStream().map(context -> {
            eventSendingService.sendDomainEventAsync(context);
            return context.getFuture();
        }).collect(Collectors.toList());
        CompletableFuture.allOf(futures.toArray(new CompletableFuture[0]));

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
