package com.damon.cqrs.sample.trade_matching.api.event;

import com.damon.cqrs.domain.Event;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class StockOrderCancelledEvent extends Event {
    private Long orderId;
    private Long price;
    /**
     * 1 买入  0 卖出
     */
    private int type;

    public StockOrderCancelledEvent(Long orderId, int type, Long price) {
        this.orderId = orderId;
        this.type = type;
        this.price = price;
    }

    public StockOrderCancelledEvent() {
    }

}
