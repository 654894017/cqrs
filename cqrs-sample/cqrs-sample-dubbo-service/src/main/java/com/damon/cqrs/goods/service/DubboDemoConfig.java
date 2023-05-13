package com.damon.cqrs.goods.service;

import com.damon.cqrs.*;
import com.damon.cqrs.event.EventCommittingService;
import com.damon.cqrs.event_store.DataSourceMapping;
import com.damon.cqrs.event_store.DefaultEventShardingRouting;
import com.damon.cqrs.event_store.MysqlEventOffset;
import com.damon.cqrs.event_store.MysqlEventStore;
import com.damon.cqrs.store.IEventOffset;
import com.damon.cqrs.store.IEventStore;
import com.google.common.collect.Lists;
import com.zaxxer.hikari.HikariDataSource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.annotation.Order;

import javax.sql.DataSource;

@Configuration
public class DubboDemoConfig {

    private final String GOODS_EVENT_QUEUE = "goods_event_queue";

//    @Bean
//    public GoodsEventListener listener() throws MQClientException {
//        return new GoodsEventListener(
//                "localhost:9876",
//                GOODS_EVENT_QUEUE,
//                "test_group",
//                50,
//                50,
//                1024
//        );
//    }

    @Bean
    @Order(-1)
    public DataSource dataSource() {
        HikariDataSource dataSource = new HikariDataSource();
        dataSource.setJdbcUrl("jdbc:mysql://127.0.0.1:3307/cqrs?serverTimezone=UTC&rewriteBatchedStatements=true");
        dataSource.setUsername("root");
        dataSource.setPassword("root");
        dataSource.setMaximumPoolSize(20);
        dataSource.setMinimumIdle(20);
        dataSource.setDriverClassName(com.mysql.cj.jdbc.Driver.class.getTypeName());
        return dataSource;
    }

    @Bean
    @Order(2)
    public IEventStore eventStore(@Autowired DataSource dataSource) {
        return new MysqlEventStore(Lists.newArrayList(
                DataSourceMapping.builder().dataSourceName("ds0").tableNumber(2).dataSource(dataSource).build()),
                16, new DefaultEventShardingRouting()
        );
    }

    @Bean
    @Order(2)
    public IEventOffset eventOffset(@Autowired DataSource dataSource) {
        return new MysqlEventOffset(Lists.newArrayList(
                DataSourceMapping.builder().dataSourceName("ds0").tableNumber(2).dataSource(dataSource).build()
        ));
    }

//    @Bean
//    @Order(3)
//    public DefaultEventSendingShceduler eventSendingShceduler(@Autowired IEventStore store, @Autowired IEventOffset offset) throws MQClientException {
//        DefaultMQProducer producer = new DefaultMQProducer();
//        producer.setNamesrvAddr("localhost:9876");
//        producer.setProducerGroup("test");
//        producer.setVipChannelEnabled(false);
//        producer.start();
//        RocketMQSendSyncService rocketmqService = new RocketMQSendSyncService(producer, GOODS_EVENT_QUEUE, 15000L);
//        EventSendingService sendingService = new EventSendingService(rocketmqService, 50, 1024);
//        return new DefaultEventSendingShceduler(store, offset, sendingService, 5);
//    }


    @Bean
    public CqrsConfig config(@Autowired IEventStore store) {
        IAggregateCache aggregateCache = new DefaultAggregateCaffeineCache(1024 * 1024, 30);
        AggregateSlotLock aggregateSlotLock = new AggregateSlotLock(4096);
        EventCommittingService service = new EventCommittingService(store, 4,
                1024, 16, 32,
                new AggregateRecoveryService(store, aggregateCache, aggregateSlotLock)
        );
        return CqrsConfig.builder().aggregateSnapshootService(
                        new DefaultAggregateSnapshootService(2, 5)
                ).aggregateCache(aggregateCache)//.beanCopy(new DefaultCglibBeanCopy())
                .aggregateSlotLock(aggregateSlotLock)
                .eventCommittingService(service).eventStore(store).build();
    }

}
