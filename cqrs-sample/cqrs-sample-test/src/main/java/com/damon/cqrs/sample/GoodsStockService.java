package com.damon.cqrs.sample;

import java.util.Date;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CountDownLatch;

import org.apache.rocketmq.client.exception.MQClientException;
import org.springframework.jdbc.core.JdbcTemplate;

import com.damon.cqrs.DefaultAggregateCache;
import com.damon.cqrs.DefaultAggregateSnapshootService;
import com.damon.cqrs.DomainService;
import com.damon.cqrs.EventCommittingService;
import com.damon.cqrs.IAggregateCache;
import com.damon.cqrs.IAggregateSnapshootService;
import com.damon.cqrs.IEventStore;
import com.damon.cqrs.store.MysqlEventStore;
import com.damon.cqrs.utils.IdWorker;
import com.zaxxer.hikari.HikariDataSource;

public class GoodsStockService extends DomainService<Goods> {

    public GoodsStockService(EventCommittingService eventCommittingService) {
        super(eventCommittingService);
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

    public static HikariDataSource dataSource() {
        HikariDataSource dataSource = new HikariDataSource();
        dataSource.setJdbcUrl("jdbc:mysql://127.0.0.1:3306/cqrs?serverTimezone=UTC&rewriteBatchedStatements=true");
        dataSource.setUsername("root");
        dataSource.setPassword("root");
        dataSource.setMaximumPoolSize(50);
        dataSource.setMinimumIdle(50);
        dataSource.setDriverClassName(com.mysql.cj.jdbc.Driver.class.getTypeName());
        return dataSource;
    }

    public static EventCommittingService init() throws MQClientException {

        IEventStore store = new MysqlEventStore(new JdbcTemplate(dataSource()));
        IAggregateSnapshootService aggregateSnapshootService = new DefaultAggregateSnapshootService(50, 5);
        IAggregateCache aggregateCache = new DefaultAggregateCache(1024 * 1024, 30);
//        DefaultMQProducer producer = new DefaultMQProducer();
//        producer.setNamesrvAddr("localhost:9876");
//        producer.setProducerGroup("test");
//        producer.start();
//        RocketMQSendService rocketmqService = new RocketMQSendService(producer, "TTTTTT", 5);
//        new DefaultEventSendingShceduler(store, rocketmqService, 5, 5);
        return new EventCommittingService(store, aggregateSnapshootService, aggregateCache, 1024, 1024);

    }

    public static void main(String[] args) throws InterruptedException, MQClientException {
        EventCommittingService committingService = init();
        GoodsStockService goodsStockService = new GoodsStockService(committingService);
        GoodsAddCommand command1 = new GoodsAddCommand(IdWorker.getId(), 2, "iphone 6 plus", 1000);
        GoodsAddCommand command2 = new GoodsAddCommand(IdWorker.getId(), 4, "iphone 7 plus", 1000);
        GoodsAddCommand command3 = new GoodsAddCommand(IdWorker.getId(), 5, "iphone 8 plus", 1000);
        GoodsAddCommand command5 = new GoodsAddCommand(IdWorker.getId(), 3, "iphone 9 plus", 1000);
        GoodsAddCommand command6 = new GoodsAddCommand(IdWorker.getId(), 1, "iphone 10s plus", 1000);
        GoodsAddCommand command7 = new GoodsAddCommand(IdWorker.getId(), 6, "iphone 11s plus", 1000);
        GoodsAddCommand command8 = new GoodsAddCommand(IdWorker.getId(), 7, "iphone 12s plus", 1000);
        goodsStockService.process(command1, () -> new Goods(2, command1.getName(), command1.getNumber())).join();
        goodsStockService.process(command2, () -> new Goods(4, command2.getName(), command2.getNumber())).join();
        goodsStockService.process(command3, () -> new Goods(5, command3.getName(), command3.getNumber())).join();
        goodsStockService.process(command5, () -> new Goods(3, command3.getName(), command3.getNumber())).join();
        goodsStockService.process(command6, () -> new Goods(1, command3.getName(), command3.getNumber())).join();
        goodsStockService.process(command7, () -> new Goods(6, command3.getName(), command3.getNumber())).join();
        goodsStockService.process(command8, () -> new Goods(7, command3.getName(), command3.getNumber())).join();

        CountDownLatch latch = new CountDownLatch(7 * 200 * 4000);
        System.out.println(new Date());
        for (int i = 0; i < 200; i++) {
            new Thread(() -> {
                for (int count = 0; count < 4000; count++) {
                    GoodsStockAddCommand command = new GoodsStockAddCommand(IdWorker.getId(), 1);
                    CompletableFuture<Goods> future = goodsStockService.process(command, goods -> goods.addStock(1));
                    try {
                        future.join();
                    } finally {
                        latch.countDown();
                    }
                }
            }).start();
        }

        for (int i = 0; i < 200; i++) {
            new Thread(() -> {
                for (int count = 0; count < 4000; count++) {
                    GoodsStockAddCommand command = new GoodsStockAddCommand(IdWorker.getId(), 7);
                    CompletableFuture<Goods> future = goodsStockService.process(command, goods -> goods.addStock(1));
                    try {
                        future.join();
                    } finally {
                        latch.countDown();
                    }
                }
            }).start();
        }

        for (int i = 0; i < 200; i++) {
            new Thread(() -> {
                for (int count = 0; count < 4000; count++) {
                    GoodsStockAddCommand command = new GoodsStockAddCommand(IdWorker.getId(), 6);
                    CompletableFuture<Goods> future = goodsStockService.process(command, goods -> goods.addStock(1));
                    try {
                        future.join();
                    } finally {
                        latch.countDown();
                    }
                }
            }).start();
        }

        for (int i = 0; i < 200; i++) {
            new Thread(() -> {
                for (int count = 0; count < 4000; count++) {
                    GoodsStockAddCommand command = new GoodsStockAddCommand(IdWorker.getId(), 5);
                    CompletableFuture<Goods> future = goodsStockService.process(command, goods -> goods.addStock(1));
                    try {
                        future.join();
                    } finally {
                        latch.countDown();
                    }
                }
            }).start();
        }

        for (int i = 0; i < 200; i++) {
            new Thread(() -> {
                for (int count = 0; count < 4000; count++) {
                    GoodsStockAddCommand command = new GoodsStockAddCommand(IdWorker.getId(), 4);
                    try {
                        CompletableFuture<Goods> future = goodsStockService.process(command, goods -> goods.addStock(1), 1000);
                        future.join();
                    } finally {
                        latch.countDown();
                    }
                }
            }).start();
        }
        for (int i = 0; i < 200; i++) {
            new Thread(() -> {
                for (int count = 0; count < 4000; count++) {
                    GoodsStockAddCommand command = new GoodsStockAddCommand(IdWorker.getId(), 2);
                    CompletableFuture<Goods> future = goodsStockService.process(command, goods -> goods.addStock(1), 5);
                    try {
                        future.join();
                    } finally {
                        latch.countDown();
                    }
                }
            }).start();
        }

        for (int i = 0; i < 200; i++) {
            new Thread(() -> {
                for (int count = 0; count < 4000; count++) {
                    GoodsStockAddCommand command = new GoodsStockAddCommand(IdWorker.getId(), 3);
                    CompletableFuture<Goods> future = goodsStockService.process(command, goods -> goods.addStock(1), 5);
                    try {
                        future.join();
                    } finally {
                        latch.countDown();
                    }
                }
            }).start();
        }

        latch.await();

        System.out.println(new Date());

    }

}
