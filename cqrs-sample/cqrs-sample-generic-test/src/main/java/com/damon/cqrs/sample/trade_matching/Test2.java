package com.damon.cqrs.sample.trade_matching;

import cn.hutool.core.util.IdUtil;
import com.damon.cqrs.config.CqrsConfig;
import com.damon.cqrs.sample.TestConfig;
import com.damon.cqrs.sample.trade_matching.api.IStockCommandService;
import com.damon.cqrs.sample.trade_matching.api.cmd.StockOrderMatchCmd;
import com.damon.cqrs.sample.trade_matching.domain.StockCommandService;

import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class Test2 {
    public static void main(String[] args) throws InterruptedException {

        CqrsConfig cqrsConfig = TestConfig.init();
        IStockCommandService stockCommandService = new StockCommandService(cqrsConfig);
        CountDownLatch countDownLatch = new CountDownLatch(800 * 5000);
        ExecutorService service1 = Executors.newFixedThreadPool(100);

        long start = System.currentTimeMillis();
        for (int i = 0; i < 1; i++) {
            service1.submit(() -> {
                for (int k = 0; k < 1; k++) {
//                    StockBuyCmd buyOrderCmd = new StockBuyCmd(IdUtil.getSnowflakeNextId(), 10000L, 1);
//                    Long orderId = IdUtil.getSnowflakeNextId();
//                    buyOrderCmd.setOrderId(orderId);
//                    buyOrderCmd.setNumber(1000);
//                    buyOrderCmd.setPrice(110L);
//                    stockCommandService.buy(buyOrderCmd);
//                    System.out.println("buy");
//                    StockSellCmd orderSellCmd = new StockSellCmd(IdUtil.getSnowflakeNextId(), 10000L, 1);
//                    orderSellCmd.setOrderId(IdUtil.getSnowflakeNextId());
//                    orderSellCmd.setNumber(500);
//                    orderSellCmd.setPrice(100L);
//                    stockCommandService.sell(orderSellCmd);
                    stockCommandService.match(new StockOrderMatchCmd(IdUtil.getSnowflakeNextId(), 10000L));
//                    stockCommandService.cancel(new StockOrderCancelCmd(IdUtil.getSnowflakeNextId(), 10000L, orderId, 1));
                    countDownLatch.countDown();
                }
            });
        }
        countDownLatch.await();
        System.out.println("耗时：" + (System.currentTimeMillis() - start));
        Thread.sleep(100000);
    }
}
