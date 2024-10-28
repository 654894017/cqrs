package com.damon.cqrs.sample.trade_matching.domain.event;

import com.damon.cqrs.domain.Event;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class OrderBoughtEvent extends Event {
    private Long stockId;
    private Long entrustPrice;
    private Long buyPrice;
    private Long orderId;
    private Integer originalNumber;
    private Integer tradingNumber;
    private Boolean isDone;

    public OrderBoughtEvent(Long stockId, Long entrustPrice, Long buyPrice, Long orderId, Integer originalNumber, Integer tradingNumber, Boolean isDone) {
        this.stockId = stockId;
        this.entrustPrice = entrustPrice;
        this.buyPrice = buyPrice;
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
