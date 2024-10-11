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

public class Test2 {
    public static void main(String[] args) throws InterruptedException {

        CqrsConfig cqrsConfig = TestConfig.init();
        StockCommandService service = new StockCommandService(cqrsConfig);
        CountDownLatch countDownLatch = new CountDownLatch(200*5000);
        long start = System.currentTimeMillis();
        for(int i=0;i<200;i++){
            new Thread(()->{
                for (int k = 0; k < 5000; k++) {
                    StockBuyCmd buyOrderCmd = new StockBuyCmd(IdUtil.getSnowflakeNextId(), 10000L);
                    buyOrderCmd.setOrderId(IdUtil.getSnowflakeNextId());
                    buyOrderCmd.setNumber(1000);
                    buyOrderCmd.setPrice(100L);
                    int result = service.buy(buyOrderCmd);
                    countDownLatch.countDown();
                    //System.out.println(result);
                }
            }).start();

        }
        countDownLatch.await();
        System.out.println("耗时："+(System.currentTimeMillis()-start));

        Thread.sleep(100000);
    }
}
