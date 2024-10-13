package com.damon.cqrs.sample.trade_matching.event;

import com.damon.cqrs.domain.Event;
import com.damon.cqrs.sample.trade_matching.aggregate.StockSellOrder;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class StockSelleEntrustSucceedEvent extends Event {
    private StockSellOrder sellOrder;

    public StockSelleEntrustSucceedEvent(StockSellOrder sellOrder) {
        this.sellOrder = sellOrder;
    }

    public StockSelleEntrustSucceedEvent() {
    }
}
