package com.damon.cqrs.sample.trade_matching.api.event;

import com.damon.cqrs.domain.Event;
import com.damon.cqrs.sample.trade_matching.domain.aggregate.StockBuyOrder;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class OrderBuyEntrustSucceedEvent extends Event {


    private StockBuyOrder buyOrder;

    public OrderBuyEntrustSucceedEvent(StockBuyOrder buyOrder) {
        this.buyOrder = buyOrder;
    }

    public OrderBuyEntrustSucceedEvent() {
    }

}
