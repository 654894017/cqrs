package com.damon.cqrs.sample.trade_matching.event;

import com.damon.cqrs.domain.Event;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class StockBoughtEvent extends Event {
    private Long stockId;
    private Long price;
    private Long orderId;
    private Integer originalNumber;
    private Integer tradingNumber;
    private Boolean finished;

    public StockBoughtEvent(Long stockId, Long price, Long orderId, Integer originalNumber, Integer tradingNumber, Boolean finished) {
        this.stockId = stockId;
        this.price = price;
        this.orderId = orderId;
        this.originalNumber = originalNumber;
        this.tradingNumber = tradingNumber;
        this.finished = finished;
    }

    public StockBoughtEvent() {
    }

    public Boolean isFinished() {
        return finished;
    }
}
