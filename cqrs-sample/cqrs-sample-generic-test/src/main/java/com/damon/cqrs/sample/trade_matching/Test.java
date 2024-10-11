package com.damon.cqrs.sample.trade_matching;

import cn.hutool.core.util.IdUtil;
import com.damon.cqrs.config.CqrsConfig;
import com.damon.cqrs.sample.TestConfig;
import com.damon.cqrs.sample.trade_matching.cmd.StockBuyCmd;
import com.damon.cqrs.sample.trade_matching.cmd.StockMatchCmd;
import com.damon.cqrs.sample.trade_matching.cmd.StockSellCmd;

import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class Test {
    public static void main(String[] args) throws InterruptedException {

        CqrsConfig cqrsConfig = TestConfig.init();
        StockCommandService service = new StockCommandService(cqrsConfig);

        for (int k = 0; k < 200; k++) {
            new Thread(() -> {
                for (int i = 0; i < 1000; i++) {
                    StockBuyCmd buyOrderCmd = new StockBuyCmd(IdUtil.getSnowflakeNextId(), 10000L);
                    buyOrderCmd.setOrderId(IdUtil.getSnowflakeNextId());
                    buyOrderCmd.setNumber(1000);
                    buyOrderCmd.setPrice(100L);
                    service.buy(buyOrderCmd);
                }
            }).start();
        }
        for (int k = 0; k < 200; k++) {
            new Thread(() -> {
                for (int i = 0; i < 1000; i++) {
                    StockSellCmd orderSellCmd = new StockSellCmd(IdUtil.getSnowflakeNextId(), 10000L);
                    orderSellCmd.setOrderId(IdUtil.getSnowflakeNextId());
                    orderSellCmd.setNumber(1000);
                    orderSellCmd.setPrice(100L);
                    service.sell(orderSellCmd);
                }
            }).start();
        }
        //Thread.sleep(8000);

        ExecutorService service2 = Executors.newFixedThreadPool(100);
        CountDownLatch latch = new CountDownLatch(100000);
        long start = System.currentTimeMillis();
        for (int i = 0; i < 100; i++) {
            service2.submit(() -> {
                for (; ; ) {
                    int result = service.match(new StockMatchCmd(IdUtil.getSnowflakeNextId(), 10000L));
                    if (result != -1) {
                        latch.countDown();
                    } else {
                        try {
                           // System.out.println(11);
                            //Thread.sleep(10);
                        } catch (Exception e) {
                        }
                    }
                }
            });
        }
        latch.await();
        System.out.println(System.currentTimeMillis() - start);
        System.out.println(1);
    }
}
