package com.damon.cqrs.sample.trade_matching.api.event;

import com.damon.cqrs.domain.Event;
import com.damon.cqrs.sample.trade_matching.domain.aggregate.StockBuyOrder;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class StockBuyEntrustSucceedEvent extends Event {


    private StockBuyOrder buyOrder;

    public StockBuyEntrustSucceedEvent(StockBuyOrder buyOrder) {
        this.buyOrder = buyOrder;
    }

    public StockBuyEntrustSucceedEvent() {
    }

    public boolean isLimitOrder() {
        return buyOrder.getType() == 1;
    }

    public boolean isMarketOrder() {
        return buyOrder.getType() == 0;
    }
}
