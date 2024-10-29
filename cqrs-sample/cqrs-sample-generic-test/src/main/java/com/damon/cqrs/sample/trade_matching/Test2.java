package com.damon.cqrs.sample.trade_matching;

import cn.hutool.core.util.IdUtil;
import com.damon.cqrs.config.CqrsConfig;
import com.damon.cqrs.sample.TestConfig;
import com.damon.cqrs.sample.trade_matching.domain.IStockCommandService;
import com.damon.cqrs.sample.trade_matching.domain.StockCommandService;
import com.damon.cqrs.sample.trade_matching.domain.cmd.*;

public class Test2 {
    public static void main(String[] args) throws InterruptedException {
        CqrsConfig cqrsConfig = TestConfig.init();
        IStockCommandService stockCommandService = new StockCommandService(cqrsConfig);
        for (int i = 0; i < 1; i++) {
            StockBuyCmd buyOrderCmd = new StockBuyCmd(IdUtil.getSnowflakeNextId(), 10000L);
            buyOrderCmd.setOrderId(IdUtil.getSnowflakeNextId());
            buyOrderCmd.setNumber(1000);
            buyOrderCmd.setPrice(101L);
            stockCommandService.buy(buyOrderCmd);
        }
        for (int i = 0; i < 1; i++) {
            StockSellCmd orderSellCmd = new StockSellCmd(IdUtil.getSnowflakeNextId(), 10000L);
            orderSellCmd.setOrderId(IdUtil.getSnowflakeNextId());
            orderSellCmd.setNumber(1000);
            orderSellCmd.setPrice(99L);
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
        System.out.println(stockCommandService.get(new StockGetCmd(IdUtil.getSnowflakeNextId(), 10000L)).getRealtimePrice());
    }
}
