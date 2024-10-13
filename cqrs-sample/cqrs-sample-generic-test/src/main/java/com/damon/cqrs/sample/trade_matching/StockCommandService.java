package com.damon.cqrs.sample.trade_matching;

import com.damon.cqrs.command.CommandService;
import com.damon.cqrs.config.CqrsConfig;
import com.damon.cqrs.sample.trade_matching.aggregate.Stock;
import com.damon.cqrs.sample.trade_matching.cmd.StockBuyCmd;
import com.damon.cqrs.sample.trade_matching.cmd.StockGetCmd;
import com.damon.cqrs.sample.trade_matching.cmd.StockMatchCmd;
import com.damon.cqrs.sample.trade_matching.cmd.StockSellCmd;

import java.util.concurrent.CompletableFuture;

public class StockCommandService extends CommandService<Stock> implements IStockCommandService {
    public StockCommandService(CqrsConfig cqrsConfig) {
        super(cqrsConfig);
    }

    @Override
    public int match(StockMatchCmd cmd) {
        return super.process(cmd, stock -> stock.match(cmd)).join();
    }

    @Override
    public int buy(StockBuyCmd cmd) {
        return super.process(cmd, stock -> stock.buy(cmd)).join();
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
    public CompletableFuture<Stock> getAggregateSnapshot(long aggregateId, Class<Stock> classes) {
        Stock stock = new Stock(aggregateId);
        return CompletableFuture.completedFuture(stock);
    }
}
