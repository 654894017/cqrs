package com.damon.cqrs.sample;


import com.damon.cqrs.*;
import com.damon.cqrs.event_store.MysqlEventOffset;
import com.damon.cqrs.event_store.MysqlEventStore;
import com.damon.cqrs.mq.DefaultMQProducer;
import com.damon.cqrs.mq.RocketMQSendSyncService;
import com.damon.cqrs.utils.IdWorker;
import com.zaxxer.hikari.HikariDataSource;
import org.apache.rocketmq.client.exception.MQClientException;

import java.util.Date;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CountDownLatch;

public class GoodsService extends AbstractDomainService<Goods> {

    public GoodsService(EventCommittingService eventCommittingService) {
        super(eventCommittingService);
    }

    public static HikariDataSource dataSource() {
        HikariDataSource dataSource = new HikariDataSource();
        dataSource.setJdbcUrl("jdbc:mysql://localhost:3306/cqrs?serverTimezone=UTC&rewriteBatchedStatements=true");
        dataSource.setUsername("root");
        dataSource.setPassword("root");
        dataSource.setMaximumPoolSize(5);
        dataSource.setMinimumIdle(5);
        dataSource.setDriverClassName(com.mysql.cj.jdbc.Driver.class.getTypeName());
        return dataSource;
    }

    public static EventCommittingService init() throws MQClientException {

        IEventStore store = new MysqlEventStore(dataSource());
        IEventOffset offset = new MysqlEventOffset(dataSource());
        IAggregateSnapshootService aggregateSnapshootService = new DefaultAggregateSnapshootService(50, 5);
        IAggregateCache aggregateCache = new DefaultAggregateGuavaCache(1024 * 1024, 30);
        DefaultMQProducer producer = new DefaultMQProducer();
        producer.setNamesrvAddr("localhost:9876");
        producer.setProducerGroup("test");
        producer.start();
        RocketMQSendSyncService rocketmqService = new RocketMQSendSyncService(producer, "TTTTTT", 5);
        EventSendingService sendingService = new EventSendingService(rocketmqService, 50, 1024);
        new DefaultEventSendingShceduler(store, offset, sendingService, 5, 5);
        return new EventCommittingService(store, aggregateSnapshootService, aggregateCache, 1024, 2048);

    }

    public static void main(String[] args) throws InterruptedException, MQClientException {
        EventCommittingService committingService = init();
        GoodsService goodsStockService = new GoodsService(committingService);
//        GoodsAddCommand command1 = new GoodsAddCommand(IdWorker.getId(), 2, "iphone 6 plus", 1000);
        GoodsCreateCommand command2 = new GoodsCreateCommand(IdWorker.getId(), 2, "iphone 7 plus", 1000);
//        GoodsAddCommand command3 = new GoodsAddCommand(IdWorker.getId(), 5, "iphone 8 plus", 1000);
//        GoodsAddCommand command5 = new GoodsAddCommand(IdWorker.getId(), 3, "iphone 9 plus", 1000);
        GoodsCreateCommand command1 = new GoodsCreateCommand(IdWorker.getId(), 1, "iphone 10s plus", 1000);
        GoodsCreateCommand command6 = new GoodsCreateCommand(IdWorker.getId(), 6, "iphone 11s plus", 1000);
        GoodsCreateCommand command7 = new GoodsCreateCommand(IdWorker.getId(), 7, "iphone 12s plus", 1000);
//        goodsStockService.process(command1, () -> new Goods(2, command1.getName(), command1.getNumber())).join();
        goodsStockService.process(command2, () -> new Goods(2, command2.getName(), command2.getNumber())).join();
//        goodsStockService.process(command3, () -> new Goods(5, command3.getName(), command3.getNumber())).join();
//        goodsStockService.process(command5, () -> new Goods(3, command3.getName(), command3.getNumber())).join();
        goodsStockService.process(command1, () -> new Goods(1, command1.getName(), command1.getNumber())).join();
        goodsStockService.process(command7, () -> new Goods(7, command7.getName(), command7.getNumber())).join();
        goodsStockService.process(command6, () -> new Goods(6, command6.getName(), command6.getNumber())).join();

        CountDownLatch latch = new CountDownLatch(2 * 200 * 2000);
        Date startDate = new Date();
        System.out.println(new Date());
        for (int i = 0; i < 200; i++) {
            new Thread(() -> {
                for (int count = 0; count < 2000; count++) {
                    GoodsStockAddCommand command = new GoodsStockAddCommand(IdWorker.getId(), 1);
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

        for (int i = 0; i < 200; i++) {
            new Thread(() -> {
                for (int count = 0; count < 2000; count++) {
                    GoodsStockAddCommand command = new GoodsStockAddCommand(IdWorker.getId(), 7);
                    CompletableFuture<Integer> future = goodsStockService.process(command, goods -> goods.addStock(1));
                    try {
                        future.join();
                    } finally {
                        latch.countDown();
                    }
                }
            }).start();
        }
//
//        for (int i = 0; i < 500; i++) {
//            new Thread(() -> {
//                for (int count = 0; count < 1000; count++) {
//                    GoodsStockAddCommand command = new GoodsStockAddCommand(IdWorker.getId(), 6);
//                    CompletableFuture<Integer> future = goodsStockService.process(command, goods -> goods.addStock(1));
//                    try {
//                        int status = future.join();
//                      //  System.out.println(status);
//                    } finally {
//                        latch.countDown();
//                    }
//                }
//            }).start();
//        }
//        for (int i = 0; i < 500; i++) {
//            new Thread(() -> {
//                for (int count = 0; count < 1000; count++) {
//                    GoodsStockAddCommand command = new GoodsStockAddCommand(IdWorker.getId(), 2);
//                    CompletableFuture<Integer> future = goodsStockService.process(command, goods -> goods.addStock(1));
//                    try {
//                        int status = future.join();
//                      //  System.out.println(status);
//                    } finally {
//                        latch.countDown();
//                    }
//                }
//            }).start();
//        }
//
//        for (int i = 0; i < 200; i++) {
//            new Thread(() -> {
//                for (int count = 0; count < 4000; count++) {
//                    GoodsStockAddCommand command = new GoodsStockAddCommand(IdWorker.getId(), 5);
//                    CompletableFuture<Goods> future = goodsStockService.process(command, goods -> goods.addStock(1));
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
//                for (int count = 0; count < 4000; count++) {
//                    GoodsStockAddCommand command = new GoodsStockAddCommand(IdWorker.getId(), 4);
//                    try {
//                        CompletableFuture<Goods> future = goodsStockService.process(command, goods -> goods.addStock(1), 1000);
//                        future.join();
//                    } finally {
//                        latch.countDown();
//                    }
//                }
//            }).start();
//        }
//        for (int i = 0; i < 200; i++) {
//            new Thread(() -> {
//                for (int count = 0; count < 4000; count++) {
//                    GoodsStockAddCommand command = new GoodsStockAddCommand(IdWorker.getId(), 2);
//                    CompletableFuture<Goods> future = goodsStockService.process(command, goods -> goods.addStock(1), 5);
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
//                for (int count = 0; count < 4000; count++) {
//                    GoodsStockAddCommand command = new GoodsStockAddCommand(IdWorker.getId(), 3);
//                    CompletableFuture<Goods> future = goodsStockService.process(command, goods -> goods.addStock(1), 5);
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
