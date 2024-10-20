package com.damon.cqrs.sample.trade_matching;

import cn.hutool.core.util.IdUtil;
import com.damon.cqrs.config.CqrsConfig;
import com.damon.cqrs.sample.TestConfig;
import com.damon.cqrs.sample.trade_matching.api.IStockCommandService;
import com.damon.cqrs.sample.trade_matching.api.cmd.StockBuyCmd;
import com.damon.cqrs.sample.trade_matching.api.cmd.StockGetCmd;
import com.damon.cqrs.sample.trade_matching.api.cmd.StockOrderMatchCmd;
import com.damon.cqrs.sample.trade_matching.api.cmd.StockSellCmd;
import com.damon.cqrs.sample.trade_matching.domain.StockCommandService;
import com.damon.cqrs.sample.trade_matching.domain.aggregate.Stock;

import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.atomic.AtomicLong;

public class Test {
    public static void main(String[] args) throws InterruptedException {

        CqrsConfig cqrsConfig = TestConfig.init();
        IStockCommandService stockCommandService = new StockCommandService(cqrsConfig);
        ExecutorService service1 = Executors.newVirtualThreadPerTaskExecutor();
        for (int k = 0; k < 400; k++) {
            service1.submit(() -> {
                for (int i = 0; i < 5000; i++) {
                    StockBuyCmd buyOrderCmd = new StockBuyCmd(IdUtil.getSnowflakeNextId(), 10000L, 0);
                    buyOrderCmd.setOrderId(IdUtil.getSnowflakeNextId());
                    buyOrderCmd.setNumber(1000);
                    buyOrderCmd.setPrice(100L);
                    stockCommandService.buy(buyOrderCmd);
                }
            });
        }
        for (int k = 0; k < 400; k++) {
            service1.submit(() -> {
                for (int i = 0; i < 5000; i++) {
                    StockBuyCmd buyOrderCmd = new StockBuyCmd(IdUtil.getSnowflakeNextId(), 10001L, 0);
                    buyOrderCmd.setOrderId(IdUtil.getSnowflakeNextId());
                    buyOrderCmd.setNumber(1000);
                    buyOrderCmd.setPrice(100L);
                    stockCommandService.buy(buyOrderCmd);
                }
            });
        }
        for (int k = 0; k < 400; k++) {
            service1.submit(() -> {
                for (int i = 0; i < 5000; i++) {
                    StockBuyCmd buyOrderCmd = new StockBuyCmd(IdUtil.getSnowflakeNextId(), 10002L, 0);
                    buyOrderCmd.setOrderId(IdUtil.getSnowflakeNextId());
                    buyOrderCmd.setNumber(1000);
                    buyOrderCmd.setPrice(100L);
                    stockCommandService.buy(buyOrderCmd);
                }
            });
        }
        for (int k = 0; k < 400; k++) {
            service1.submit(() -> {
                for (int i = 0; i < 5000; i++) {
                    StockBuyCmd buyOrderCmd = new StockBuyCmd(IdUtil.getSnowflakeNextId(), 10003L, 0);
                    buyOrderCmd.setOrderId(IdUtil.getSnowflakeNextId());
                    buyOrderCmd.setNumber(1000);
                    buyOrderCmd.setPrice(100L);
                    stockCommandService.buy(buyOrderCmd);
                }
            });
        }
        ExecutorService service2 = Executors.newVirtualThreadPerTaskExecutor();

        for (int k = 0; k < 400; k++) {
            service2.submit(() -> {
                for (int i = 0; i < 5000; i++) {
                    StockSellCmd orderSellCmd = new StockSellCmd(IdUtil.getSnowflakeNextId(), 10000L, 0);
                    orderSellCmd.setOrderId(IdUtil.getSnowflakeNextId());
                    orderSellCmd.setNumber(1000);
                    orderSellCmd.setPrice(100L);
                    stockCommandService.sell(orderSellCmd);
                }
            });
        }
        for (int k = 0; k < 400; k++) {
            service2.submit(() -> {
                for (int i = 0; i < 5000; i++) {
                    StockSellCmd orderSellCmd = new StockSellCmd(IdUtil.getSnowflakeNextId(), 10001L, 0);
                    orderSellCmd.setOrderId(IdUtil.getSnowflakeNextId());
                    orderSellCmd.setNumber(1000);
                    orderSellCmd.setPrice(100L);
                    stockCommandService.sell(orderSellCmd);
                }
            });
        }
        for (int k = 0; k < 400; k++) {
            service2.submit(() -> {
                for (int i = 0; i < 5000; i++) {
                    StockSellCmd orderSellCmd = new StockSellCmd(IdUtil.getSnowflakeNextId(), 10002L, 0);
                    orderSellCmd.setOrderId(IdUtil.getSnowflakeNextId());
                    orderSellCmd.setNumber(1000);
                    orderSellCmd.setPrice(100L);
                    stockCommandService.sell(orderSellCmd);
                }
            });
        }
        for (int k = 0; k < 400; k++) {
            service2.submit(() -> {
                for (int i = 0; i < 5000; i++) {
                    StockSellCmd orderSellCmd = new StockSellCmd(IdUtil.getSnowflakeNextId(), 10003L, 0);
                    orderSellCmd.setOrderId(IdUtil.getSnowflakeNextId());
                    orderSellCmd.setNumber(1000);
                    orderSellCmd.setPrice(100L);
                    stockCommandService.sell(orderSellCmd);
                }
            });
        }
        CountDownLatch latch = new CountDownLatch(200 * 10000);
        AtomicLong count = new AtomicLong();
        long start = System.currentTimeMillis();
        ExecutorService service3 = Executors.newVirtualThreadPerTaskExecutor();
        for (int i = 0; i < 400; i++) {
            service3.submit(() -> {
                for (; ; ) {
                    int result = stockCommandService.match(new StockOrderMatchCmd(IdUtil.getSnowflakeNextId(), 10000L));
                    if (result != -1) {
                        latch.countDown();
                    } else {
                        count.addAndGet(1);
                        Thread.sleep(1);
                    }
                }
            });
        }
        for (int i = 0; i < 400; i++) {
            service3.submit(() -> {
                for (; ; ) {
                    int result = stockCommandService.match(new StockOrderMatchCmd(IdUtil.getSnowflakeNextId(), 10001L));
                    if (result != -1) {
                        latch.countDown();
                    } else {
                        count.addAndGet(1);
                        Thread.sleep(1);
                    }
                }
            });
        }
        for (int i = 0; i < 400; i++) {
            service3.submit(() -> {
                for (; ; ) {
                    int result = stockCommandService.match(new StockOrderMatchCmd(IdUtil.getSnowflakeNextId(), 10002L));
                    if (result != -1) {
                        latch.countDown();
                    } else {
                        count.addAndGet(1);
                        Thread.sleep(1);
                    }
                }
            });
        }
        for (int i = 0; i < 400; i++) {
            service3.submit(() -> {
                for (; ; ) {
                    int result = stockCommandService.match(new StockOrderMatchCmd(IdUtil.getSnowflakeNextId(), 10003L));
                    if (result != -1) {
                        latch.countDown();
                    } else {
                        count.addAndGet(1);
                        Thread.sleep(1);
                    }
                }
            });
        }
        latch.await();
        System.out.println(count.get());
        System.out.println(System.currentTimeMillis() - start);
        Stock stock = stockCommandService.get(new StockGetCmd(IdUtil.getSnowflakeNextId(), 10000L));
        System.out.println(stock.getTradeMap().size());
        System.out.println(1);
    }
}
