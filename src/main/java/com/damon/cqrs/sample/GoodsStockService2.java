package com.damon.cqrs.sample;

import java.util.Date;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CountDownLatch;

import org.springframework.jdbc.core.JdbcTemplate;

import com.damon.cqrs.DefaultAggregateCache;
import com.damon.cqrs.DefaultAggregateSnapshootService;
import com.damon.cqrs.EventCommittingService;
import com.damon.cqrs.IAggregateCache;
import com.damon.cqrs.IAggregateSnapshootService;
import com.damon.cqrs.IEventStore;
import com.damon.cqrs.domain.DomainService;
import com.damon.cqrs.store.MysqlEventStore;
import com.damon.cqrs.utils.IdWorker;
import com.zaxxer.hikari.HikariDataSource;

public class GoodsStockService2 extends DomainService<Goods> {

    public GoodsStockService2(EventCommittingService eventCommittingService) {
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
    //    System.out.println(goods.getId() + ":" + goods.getCount() + ":" + goods.getName() + ":" + goods.getVersion());
        return CompletableFuture.completedFuture(true);
    }

    public static HikariDataSource dataSource() {
        HikariDataSource dataSource = new HikariDataSource();
        dataSource.setJdbcUrl("jdbc:mysql://127.0.0.1:3306/enode?serverTimezone=UTC&rewriteBatchedStatements=true");
        dataSource.setUsername("root");
        dataSource.setPassword("root");
        dataSource.setMaximumPoolSize(200);
        dataSource.setMinimumIdle(200);
        dataSource.setDriverClassName(com.mysql.cj.jdbc.Driver.class.getTypeName());
        return dataSource;
    }

    public static EventCommittingService init() {
        IEventStore store = new MysqlEventStore(new JdbcTemplate(dataSource()));
        IAggregateSnapshootService aggregateSnapshootService = new DefaultAggregateSnapshootService(50, 5);
        IAggregateCache aggregateCache = new DefaultAggregateCache(1024*1024,30);
        return new EventCommittingService(store, aggregateSnapshootService, aggregateCache, 180, 1024);

    }

    public static void main(String[] args) throws InterruptedException {

        EventCommittingService committingService = init();
        GoodsStockService2 goodsStockService = new GoodsStockService2(committingService);
//        GoodsAddCommand command1 = new GoodsAddCommand(IdWorker.getId(), 2, "iphone 6 plus", 1000);
//        GoodsAddCommand command2 = new GoodsAddCommand(IdWorker.getId(), 4, "iphone 7 plus", 1000);
//        GoodsAddCommand command3 = new GoodsAddCommand(IdWorker.getId(), 5, "iphone 8 plus", 1000);
//        goodsStockService.process(command1, () -> new Goods(2, command1.getName(), command1.getCount()), 5).join();
//        goodsStockService.process(command2, () -> new Goods(4, command2.getName(), command2.getCount()), 5).join();
//        goodsStockService.process(command3, () -> new Goods(5, command3.getName(), command3.getCount()), 5).join();

        CountDownLatch latch = new CountDownLatch(1 * 400 * 2000);
        System.out.println(new Date());
        for (int i = 0; i < 400; i++) {
            new Thread(() -> {
                for (int count = 0; count < 2000; count++) {
                    long commandId= IdWorker.getId();
                    long aggreagetId = IdWorker.getId();
                    GoodsStockAddCommand command = new GoodsStockAddCommand(commandId, aggreagetId);
                    
                    long commandId2= IdWorker.getId();
                    GoodsStockAddCommand command2 = new GoodsStockAddCommand(commandId2, aggreagetId);
                    
                    
                    long commandId3= IdWorker.getId();

                    GoodsStockAddCommand command3 = new GoodsStockAddCommand(commandId3, aggreagetId);
                    
                    GoodsAddCommand command1 = new GoodsAddCommand(IdWorker.getId(), aggreagetId, "iphone 6 plus", 1000);
                    
                   goodsStockService.process(command1, () -> new Goods(aggreagetId, command1.getName(), command1.getCount()), 5).join();
                   CompletableFuture<Goods> g1=goodsStockService.process(command, goods->goods.addStock(command.getNumber(), "ddddd22"));
                   CompletableFuture<Goods> g2= goodsStockService.process(command2, goods->goods.addStock(command.getNumber(), "ddddd22"));
                   CompletableFuture<Goods> g3=goodsStockService.process(command3, goods->goods.addStock(command.getNumber(), "ddddd22"));
                   
                   CompletableFuture.allOf(g1,g2,g3).join();
                    
                    
                }
            }).start();
        }

        latch.await();

        System.out.println(new Date());

    }

}
