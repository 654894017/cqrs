package com.damon.cqrs.sample.trade_matching;

import cn.hutool.core.util.IdUtil;
import com.damon.cqrs.config.CqrsConfig;
import com.damon.cqrs.sample.TestConfig;
import com.damon.cqrs.sample.trade_matching.api.IStockCommandService;
import com.damon.cqrs.sample.trade_matching.api.cmd.*;
import com.damon.cqrs.sample.trade_matching.domain.StockCommandService;
import com.damon.cqrs.sample.trade_matching.domain.aggregate.Stock;

import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.atomic.AtomicLong;

public class Test2 {
    public static void main(String[] args) throws InterruptedException {
        CqrsConfig cqrsConfig = TestConfig.init();
        IStockCommandService stockCommandService = new StockCommandService(cqrsConfig);
        for (int i = 0; i < 1; i++) {
            StockBuyCmd buyOrderCmd = new StockBuyCmd(IdUtil.getSnowflakeNextId(), 10000L);
            buyOrderCmd.setOrderId(IdUtil.getSnowflakeNextId());
            buyOrderCmd.setNumber(1000);
            buyOrderCmd.setPrice(100L);
            stockCommandService.buy(buyOrderCmd);
        }
        for (int i = 0; i < 1; i++) {
            StockSellCmd orderSellCmd = new StockSellCmd(IdUtil.getSnowflakeNextId(), 10000L);
            orderSellCmd.setOrderId(IdUtil.getSnowflakeNextId());
            orderSellCmd.setNumber(1000);
            orderSellCmd.setPrice(100L);
            stockCommandService.sell(orderSellCmd);
        }

        StockMarketBuyCmd stockMarketBuyCmd = new StockMarketBuyCmd(IdUtil.getSnowflakeNextId(), 10000l);
        stockMarketBuyCmd.setNumber(500);
        stockMarketBuyCmd.setOrderId(IdUtil.getSnowflakeNextId());
        stockMarketBuyCmd.setEntrustmentType(1);
        stockCommandService.buy(stockMarketBuyCmd);

        StockMarketSellCmd stockMarketSellCmd = new StockMarketSellCmd(IdUtil.getSnowflakeNextId(), 10000l);
        stockMarketSellCmd.setNumber(500);
        stockMarketSellCmd.setOrderId(IdUtil.getSnowflakeNextId());
        stockMarketSellCmd.setEntrustmentType(0);
        stockCommandService.sell(stockMarketSellCmd);

        int result = stockCommandService.match(new StockOrderMatchCmd(IdUtil.getSnowflakeNextId(), 10000L));

    }
}
