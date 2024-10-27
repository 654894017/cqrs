package com.damon.cqrs.sample.trade_matching.api.event;

import com.damon.cqrs.domain.Event;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class OrderBoughtEvent extends Event {
    private Long stockId;
    private Long price;
    private Long orderId;
    private Integer originalNumber;
    private Integer tradingNumber;
    private Boolean isDone;

    public OrderBoughtEvent(Long stockId, Long price, Long orderId, Integer originalNumber, Integer tradingNumber, Boolean isDone) {
        this.stockId = stockId;
        this.price = price;
        this.orderId = orderId;
        this.originalNumber = originalNumber;
        this.tradingNumber = tradingNumber;
        this.isDone = isDone;
    }

    public OrderBoughtEvent() {
    }

    public Boolean isDone() {
        return isDone;
    }
}
