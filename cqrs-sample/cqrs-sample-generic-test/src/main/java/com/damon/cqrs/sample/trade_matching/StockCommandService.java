package com.damon.cqrs.sample.trade_matching;

import com.damon.cqrs.command.CommandService;
import com.damon.cqrs.config.CqrsConfig;
import com.damon.cqrs.domain.Command;
import com.damon.cqrs.sample.trade_matching.cmd.StockBuyCmd;
import com.damon.cqrs.sample.trade_matching.cmd.StockMatchCmd;
import com.damon.cqrs.sample.trade_matching.cmd.StockSellCmd;
import com.damon.cqrs.sample.trade_matching.model.Stock;

import java.util.concurrent.CompletableFuture;
import java.util.function.Function;

public class StockCommandService extends CommandService<Stock> implements IStockCommandService {
    public StockCommandService(CqrsConfig cqrsConfig) {
        super(cqrsConfig);
    }

    public int match(StockMatchCmd cmd) {
        return super.process2(cmd, stock -> stock.match(cmd)).join();
    }

    public int buy(StockBuyCmd cmd) {
        return super.process2(cmd, stock -> stock.buy(cmd)).join();
    }

    public int sell(StockSellCmd cmd) {
        return super.process2(cmd, stock -> stock.sell(cmd)).join();
    }

    @Override
    public CompletableFuture<Stock> getAggregateSnapshot(long aggregateId, Class<Stock> classes) {
        Stock stock = new Stock(aggregateId);
        stock.setVersion(1);
        return CompletableFuture.completedFuture(stock);
    }
}
