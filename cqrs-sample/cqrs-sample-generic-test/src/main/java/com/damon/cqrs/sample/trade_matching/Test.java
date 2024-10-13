package com.damon.cqrs.sample.trade_matching;

import cn.hutool.core.util.IdUtil;
import com.damon.cqrs.config.CqrsConfig;
import com.damon.cqrs.sample.TestConfig;
import com.damon.cqrs.sample.trade_matching.aggregate.Stock;
import com.damon.cqrs.sample.trade_matching.cmd.StockBuyCmd;
import com.damon.cqrs.sample.trade_matching.cmd.StockGetCmd;
import com.damon.cqrs.sample.trade_matching.cmd.StockMatchCmd;
import com.damon.cqrs.sample.trade_matching.cmd.StockSellCmd;

import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.atomic.AtomicLong;

public class Test {
    public static void main(String[] args) throws InterruptedException {

        CqrsConfig cqrsConfig = TestConfig.init();
        StockCommandService service = new StockCommandService(cqrsConfig);
        ExecutorService service1 = Executors.newVirtualThreadPerTaskExecutor();
        for (int k = 0; k < 800; k++) {
            service1.submit(() -> {
                for (int i = 0; i < 5000; i++) {
                    StockBuyCmd buyOrderCmd = new StockBuyCmd(IdUtil.getSnowflakeNextId(), 10000L);
                    buyOrderCmd.setOrderId(IdUtil.getSnowflakeNextId());
                    buyOrderCmd.setNumber(1000);
                    buyOrderCmd.setPrice(100L);
                    service.buy(buyOrderCmd);
                }
            });
        }
        ExecutorService service2 = Executors.newVirtualThreadPerTaskExecutor();

        for (int k = 0; k < 800; k++) {
            service2.submit(() -> {
                for (int i = 0; i < 5000; i++) {
                    StockSellCmd orderSellCmd = new StockSellCmd(IdUtil.getSnowflakeNextId(), 10000L);
                    orderSellCmd.setOrderId(IdUtil.getSnowflakeNextId());
                    orderSellCmd.setNumber(1000);
                    orderSellCmd.setPrice(100L);
                    service.sell(orderSellCmd);
                }
            });
        }
        CountDownLatch latch = new CountDownLatch(100 * 10000);
        AtomicLong count = new AtomicLong();
        long start = System.currentTimeMillis();
        ExecutorService service3 = Executors.newVirtualThreadPerTaskExecutor();
        for (int i = 0; i < 800; i++) {
            service3.submit(() -> {
                for (; ; ) {
                    int result = service.match(new StockMatchCmd(IdUtil.getSnowflakeNextId(), 10000L));
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
        Stock stock = service.get(new StockGetCmd(IdUtil.getSnowflakeNextId(), 10000L));
        System.out.println(stock.getTradeMap().size());
        System.out.println(1);
    }
}
