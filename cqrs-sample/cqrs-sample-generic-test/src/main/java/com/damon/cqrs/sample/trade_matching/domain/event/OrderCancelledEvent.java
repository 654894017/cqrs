package com.damon.cqrs.sample.trade_matching.domain.event;

import com.damon.cqrs.domain.Event;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class OrderCancelledEvent extends Event {
    private Long orderId;
    private Long price;
    /**
     * 1 买入  0 卖出
     */
    private int type;

    public OrderCancelledEvent(Long orderId, int type, Long price) {
        this.orderId = orderId;
        this.type = type;
        this.price = price;
    }

    public OrderCancelledEvent() {
    }

    public boolean isSellOrder() {
        return type == 0;
    }

    public boolean isBuyOrder() {
        return type == 1;
    }

}
