package com.damon.cqrs.sample.trade_matching;

import cn.hutool.core.util.IdUtil;
import com.damon.cqrs.config.CqrsConfig;
import com.damon.cqrs.sample.TestConfig;
import com.damon.cqrs.sample.trade_matching.api.IStockCommandService;
import com.damon.cqrs.sample.trade_matching.api.cmd.StockBuyCmd;
import com.damon.cqrs.sample.trade_matching.domain.StockCommandService;

import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class Test4 {
    public static void main(String[] args) throws InterruptedException {

        CqrsConfig cqrsConfig = TestConfig.init();
        IStockCommandService stockCommandService = new StockCommandService(cqrsConfig);
        ExecutorService service1 = Executors.newFixedThreadPool(1);
        for (int k = 0; k < 1; k++) {
            service1.submit(() -> {
                for (int i = 0; i < 10; i++) {
                    StockBuyCmd buyOrderCmd = new StockBuyCmd(IdUtil.getSnowflakeNextId(), 10000L, 0);
                    buyOrderCmd.setOrderId(IdUtil.getSnowflakeNextId());
                    buyOrderCmd.setNumber(1000);
                    buyOrderCmd.setPrice(100L);
                    try {
                        System.out.println(stockCommandService.buy(buyOrderCmd));
                        System.out.println(1);
                    } catch (Exception e) {
                        e.printStackTrace();
                    }

                }
            });
        }

    }
}
