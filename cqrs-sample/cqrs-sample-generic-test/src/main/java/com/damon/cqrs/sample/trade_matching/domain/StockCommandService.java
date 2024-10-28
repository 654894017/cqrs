package com.damon.cqrs.sample.trade_matching.domain;

import com.damon.cqrs.command.CommandService;
import com.damon.cqrs.config.CqrsConfig;
import com.damon.cqrs.sample.trade_matching.domain.aggregate.Stock;
import com.damon.cqrs.sample.trade_matching.domain.cmd.*;

public class StockCommandService extends CommandService<Stock> implements IStockCommandService {
    public StockCommandService(CqrsConfig cqrsConfig) {
        super(cqrsConfig);
    }

    @Override
    public int match(StockOrderMatchCmd cmd) {
        return super.process(cmd, stock -> stock.match(cmd)).join();
    }

    @Override
    public int buy(StockBuyCmd cmd) {
        return super.process(cmd, stock -> stock.buy(cmd)).join();
    }

    @Override
    public int buy(StockMarketBuyCmd cmd) {
        return super.process(cmd, stock -> stock.buy(cmd)).join();
    }

    @Override
    public int sell(StockMarketSellCmd cmd) {
        return super.process(cmd, stock -> stock.sell(cmd)).join();
    }

    @Override
    public int sell(StockSellCmd cmd) {
        return super.process(cmd, stock -> stock.sell(cmd)).join();
    }

    @Override
    public int cancel(StockOrderCancelCmd cmd) {
        return super.process(cmd, stock -> stock.cancel(cmd)).join();
    }

    @Override
    public Stock get(StockGetCmd cmd) {
        return super.process(cmd, stock -> stock).join();
    }

    @Override
    public Stock getAggregateSnapshot(long aggregateId, Class<Stock> classes) {
        Stock stock = new Stock(aggregateId);
        // 这边可以查询数据库获取上一个交易日的收盘价作为初始价格
        stock.setRealtimePrice(100L);
        // 一个档位的价格,根据上一个交易日的涨停价除以100档位(按照涨停10个点计算)
        stock.setNotchPrice(1L);
        return stock;
    }
}
