package com.damon.cqrs.sample.trade_matching.api;

import com.damon.cqrs.sample.trade_matching.api.cmd.*;
import com.damon.cqrs.sample.trade_matching.domain.aggregate.Stock;

public interface IStockCommandService {
    int match(StockOrderMatchCmd cmd);

    int buy(StockBuyCmd cmd);

    int cancel(StockOrderCancelCmd cmd);

    int sell(StockSellCmd cmd);

    Stock get(StockGetCmd cmd);
}
