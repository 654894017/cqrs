package com.damon.cqrs.sample.trade_matching.api.event;

import com.damon.cqrs.domain.Event;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class OrderSelledEvent extends Event {
    private Long price;
    private Long orderId;
    private Integer originalNumber;
    private Integer tradingNumber;
    private Boolean isDone;

    public OrderSelledEvent(Long price, Long orderId, Integer originalNumber, Integer tradingNumber, Boolean isDone) {
        this.price = price;
        this.orderId = orderId;
        this.originalNumber = originalNumber;
        this.tradingNumber = tradingNumber;
        this.isDone = isDone;
    }

    public OrderSelledEvent() {
    }

    public Boolean isDone() {
        return isDone;
    }
}
