package com.damon.cqrs.sample.trade_matching.event;

import com.damon.cqrs.domain.Event;
import com.damon.cqrs.sample.trade_matching.aggregate.StockBuyOrder;
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
}
