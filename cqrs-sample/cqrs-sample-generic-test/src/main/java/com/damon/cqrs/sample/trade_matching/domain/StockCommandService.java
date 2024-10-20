package com.damon.cqrs.sample.trade_matching.domain;

import com.damon.cqrs.command.CommandService;
import com.damon.cqrs.config.CqrsConfig;
import com.damon.cqrs.sample.trade_matching.api.IStockCommandService;
import com.damon.cqrs.sample.trade_matching.api.cmd.*;
import com.damon.cqrs.sample.trade_matching.domain.aggregate.Stock;

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
    public int cancel(StockOrderCancelCmd cmd) {
        return super.process(cmd, stock -> stock.cancel(cmd)).join();
    }

    @Override
    public int sell(StockSellCmd cmd) {
        return super.process(cmd, stock -> stock.sell(cmd)).join();
    }

    @Override
    public Stock get(StockGetCmd cmd) {
        return super.process(cmd, stock -> stock).join();
    }

    @Override
    public Stock getAggregateSnapshot(long aggregateId, Class<Stock> classes) {
        Stock stock = new Stock(aggregateId);
        return stock;
    }
}
