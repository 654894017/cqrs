package com.damon.cqrs.sample;

import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;

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
        // producer.createTopic("TTTTTT", "TTTTTT", 100);
        RocketMQSendService rocketmqService = new RocketMQSendService(producer, "TTTTTT");
//        CompletableFuture<Boolean> future = new CompletableFuture<Boolean>();
//        List<Event> events = new ArrayList<>();
//        GoodsAddEvent event = new GoodsAddEvent();
//        event.setAggregateId(1);
//        event.setAggregateType(Goods.class.getTypeName());
//        event.setCount(5);
//        event.setId(55);
//        event.setName("abcccc");
//        event.setTimestamp(ZonedDateTime.now());
//        events.add(event);
//        rocketmqService.sendMessage(Lists.newArrayList(EventSendingContext.builder().aggregateId(1).aggregateType(Goods.class.getTypeName()).future(future).events(events).build()));
//
//        future.join();
//        System.out.println(333333);

        EventSendingService eventSendingService = new EventSendingService(rocketmqService, 10, 1024);
        IEventStore store = new MysqlEventStore(new JdbcTemplate(dataSource()));
        ScheduledExecutorService service = Executors.newScheduledThreadPool(5);
        CompletableFuture<List<EventSendingContext>> future2 = store.queryWaitingSend(1);
        List<EventSendingContext> context = future2.join();
        context.forEach(c -> eventSendingService.sendDomainEventAsync(c));

//        service.scheduleAtFixedRate(new Runnable() {
//            @Override
//            public void run() {
//                CompletableFuture<List<List<Event>>> future = store.load(2, Goods.class, 1, 55555555);
//                future.thenApply(events->{
//                    events.forEach(es->{
//                        es.stream().map(event->{
//                            EventSendingContext context =  EventSendingContext.builder().aggregateId(event)
//                        });
//                        eventSendingService.sendDomainEventAsync(event);
//                    });
//                });
//            }
//        }, 5, 5, TimeUnit.SECONDS);
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
