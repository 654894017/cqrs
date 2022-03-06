package com.damon.cqrs.sample.goods;


import com.damon.cqrs.*;
import com.damon.cqrs.event.EventCommittingService;
import com.damon.cqrs.event.EventSendingService;
import com.damon.cqrs.event_store.MysqlEventOffset;
import com.damon.cqrs.event_store.MysqlEventStore;
import com.damon.cqrs.rocketmq.RocketMQSendSyncService;
import com.damon.cqrs.rocketmq.DefaultMQProducer;
import com.damon.cqrs.store.IEventOffset;
import com.damon.cqrs.store.IEventStore;
import com.damon.cqrs.utils.IdWorker;
import com.zaxxer.hikari.HikariDataSource;
import org.apache.rocketmq.client.exception.MQClientException;

import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Random;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CountDownLatch;

public class GoodsService2 extends AbstractDomainService<Goods> {

    public GoodsService2(EventCommittingService eventCommittingService) {
        super(eventCommittingService);
    }

    public static HikariDataSource dataSource() {
        HikariDataSource dataSource = new HikariDataSource();
        dataSource.setJdbcUrl("jdbc:mysql://localhost:3306/cqrs?serverTimezone=UTC&rewriteBatchedStatements=true");
        dataSource.setUsername("root");
        dataSource.setPassword("root");
        dataSource.setMaximumPoolSize(10);
        dataSource.setMinimumIdle(10);
        dataSource.setDriverClassName(com.mysql.cj.jdbc.Driver.class.getTypeName());
        return dataSource;
    }

    public static EventCommittingService init() throws MQClientException {

        IEventStore store = new MysqlEventStore(dataSource());
        IEventOffset offset = new MysqlEventOffset(dataSource());
        IAggregateSnapshootService aggregateSnapshootService = new DefaultAggregateSnapshootService(18, 5);
        IAggregateCache aggregateCache = new DefaultAggregateGuavaCache(1024 * 1024, 30);
        DefaultMQProducer producer = new DefaultMQProducer();
        producer.setNamesrvAddr("localhost:9876");
        producer.setProducerGroup("test");
        //  producer.start();
        RocketMQSendSyncService rocketmqService = new RocketMQSendSyncService(producer, "TTTTTT", 5);
        EventSendingService sendingService = new EventSendingService(rocketmqService, 50, 1024);
        //  new DefaultEventSendingShceduler(store, offset, sendingService, 5, 5);
        return new EventCommittingService(store, aggregateSnapshootService, aggregateCache, 4, 2048,16);

    }

    public static void main(String[] args) throws InterruptedException, MQClientException {
        EventCommittingService committingService = init();
        GoodsService2 goodsStockService = new GoodsService2(committingService);

        CountDownLatch downLatch = new CountDownLatch(1 * 500 * 1000);
        List<Long> ids = new ArrayList<>();
        for (int i = 0; i < 4; i++) {
            GoodsCreateCommand command1 = new GoodsCreateCommand(IdWorker.getId(), i+1, "iphone 6 plus " + i, 1000);
            System.out.println(goodsStockService.process(command1, () -> new Goods(command1.getAggregateId(), command1.getName(), command1.getNumber())).join());
            ids.add((long) (i+1));
        }
        int size = ids.size();
        Random random = new Random();

        CountDownLatch latch = new CountDownLatch(1 * 1000 * 2000);
        Date startDate = new Date();
        System.out.println(new Date());
        for (int i = 0; i < 1000; i++) {
            new Thread(() -> {
                for (int count = 0; count < 2000; count++) {
                    int index = random.nextInt(size);
                    GoodsStockAddCommand command = new GoodsStockAddCommand(IdWorker.getId(), ids.get(index));
                    CompletableFuture<Integer> future = goodsStockService.process(command, goods -> goods.addStock(1));
                    try {
                        int status = future.join();
                        //  System.out.println(status);
                    } finally {
                        latch.countDown();
                    }
                }
            }).start();
        }

//        for (int i = 0; i < 200; i++) {
//            new Thread(() -> {
//                for (int count = 0; count < 2000; count++) {
//                    GoodsStockAddCommand command = new GoodsStockAddCommand(IdWorker.getId(), 2);
//                    CompletableFuture<Integer> future = goodsStockService.process(command, goods -> goods.addStock(1));
//                    try {
//                        future.join();
//                    } finally {
//                        latch.countDown();
//                    }
//                }
//            }).start();
//        }
//
//        for (int i = 0; i < 200; i++) {
//            new Thread(() -> {
//                for (int count = 0; count < 2000; count++) {
//                    GoodsStockAddCommand command = new GoodsStockAddCommand(IdWorker.getId(), 3);
//                    CompletableFuture<Integer> future = goodsStockService.process(command, goods -> goods.addStock(1));
//                    try {
//                        int status = future.join();
//                        //  System.out.println(status);
//                    } finally {
//                        latch.countDown();
//                    }
//                }
//            }).start();
//        }
//        for (int i = 0; i < 200; i++) {
//            new Thread(() -> {
//                for (int count = 0; count < 2000; count++) {
//                    GoodsStockAddCommand command = new GoodsStockAddCommand(IdWorker.getId(), 4);
//                    CompletableFuture<Integer> future = goodsStockService.process(command, goods -> goods.addStock(1));
//                    try {
//                        int status = future.join();
//                        //  System.out.println(status);
//                    } finally {
//                        latch.countDown();
//                    }
//                }
//            }).start();
//        }
//
//        for (int i = 0; i < 200; i++) {
//            new Thread(() -> {
//                for (int count = 0; count < 2000; count++) {
//                    GoodsStockAddCommand command = new GoodsStockAddCommand(IdWorker.getId(), 5);
//                    CompletableFuture<Integer> future = goodsStockService.process(command, goods -> goods.addStock(1));
//                    try {
//                        future.join();
//                    } finally {
//                        latch.countDown();
//                    }
//                }
//            }).start();
//        }
//
//        for (int i = 0; i < 200; i++) {
//            new Thread(() -> {
//                for (int count = 0; count < 2000; count++) {
//                    GoodsStockAddCommand command = new GoodsStockAddCommand(IdWorker.getId(), 6);
//                    try {
//                        CompletableFuture<Integer> future = goodsStockService.process(command, goods -> goods.addStock(1), 1000);
//                        future.join();
//                    } finally {
//                        latch.countDown();
//                    }
//                }
//            }).start();
//        }
//        for (int i = 0; i < 200; i++) {
//            new Thread(() -> {
//                for (int count = 0; count < 2000; count++) {
//                    GoodsStockAddCommand command = new GoodsStockAddCommand(IdWorker.getId(), 7);
//                    CompletableFuture<Integer> future = goodsStockService.process(command, goods -> goods.addStock(1), 5);
//                    try {
//                        future.join();
//                    } finally {
//                        latch.countDown();
//                    }
//                }
//            }).start();
//        }
//
//        for (int i = 0; i < 200; i++) {
//            new Thread(() -> {
//                for (int count = 0; count < 2000; count++) {
//                    GoodsStockAddCommand command = new GoodsStockAddCommand(IdWorker.getId(), 8);
//                    CompletableFuture<Integer> future = goodsStockService.process(command, goods -> goods.addStock(1), 5);
//                    try {
//                        future.join();
//                    } finally {
//                        latch.countDown();
//                    }
//                }
//            }).start();
//        }

        latch.await();
        System.out.println(startDate);

        System.out.println(new Date());

    }

    @Override
    public CompletableFuture<Goods> getAggregateSnapshoot(long aggregateId, Class<Goods> classes) {
        return CompletableFuture.supplyAsync(() -> {
            return null;
        });

    }

    @Override
    public CompletableFuture<Boolean> saveAggregateSnapshoot(Goods goods) {
        System.out.println(goods.getId() + ":" + goods.getNumber() + ":" + goods.getName() + ":" + goods.getVersion());
        return CompletableFuture.completedFuture(true);
    }

}
