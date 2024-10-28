package com.damon.cqrs.sample.trade_matching.domain;

import com.damon.cqrs.sample.trade_matching.domain.aggregate.Stock;
import com.damon.cqrs.sample.trade_matching.domain.cmd.*;

public interface IStockCommandService {
    int match(StockOrderMatchCmd cmd);

    int buy(StockBuyCmd cmd);

    int cancel(StockOrderCancelCmd cmd);

    int buy(StockMarketBuyCmd cmd);

    int sell(StockMarketSellCmd cmd);

    int sell(StockSellCmd cmd);

    Stock get(StockGetCmd cmd);
}
