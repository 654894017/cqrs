package com.damon.cqrs.sample.trade_matching;

import com.damon.cqrs.sample.trade_matching.aggregate.Stock;
import com.damon.cqrs.sample.trade_matching.cmd.StockBuyCmd;
import com.damon.cqrs.sample.trade_matching.cmd.StockGetCmd;
import com.damon.cqrs.sample.trade_matching.cmd.StockMatchCmd;
import com.damon.cqrs.sample.trade_matching.cmd.StockSellCmd;

public interface IStockCommandService {
    int match(StockMatchCmd cmd);

    int buy(StockBuyCmd cmd);

    int sell(StockSellCmd cmd);

    Stock get(StockGetCmd cmd);
}
