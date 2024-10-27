package com.damon.cqrs.sample.trade_matching.api.event;

import com.damon.cqrs.domain.Event;
import com.damon.cqrs.sample.trade_matching.domain.aggregate.StockSellOrder;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class OrderSelleEntrustSucceedEvent extends Event {
    /**
     * 1 limit order  0 market order
     */
    private int type;
    private StockSellOrder sellOrder;

    public OrderSelleEntrustSucceedEvent(StockSellOrder sellOrder) {
        this.sellOrder = sellOrder;
    }

    public OrderSelleEntrustSucceedEvent() {
    }

    public boolean isLimitOrder() {
        return type == 1;
    }

    public boolean isMarketOrder() {
        return type == 0;
    }

}
