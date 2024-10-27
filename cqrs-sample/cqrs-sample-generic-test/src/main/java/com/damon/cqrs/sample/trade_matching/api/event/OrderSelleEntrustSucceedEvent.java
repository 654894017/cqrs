package com.damon.cqrs.sample.trade_matching.api.event;

import com.damon.cqrs.domain.Event;
import com.damon.cqrs.sample.trade_matching.domain.aggregate.StockSellOrder;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class OrderSelleEntrustSucceedEvent extends Event {

    private Long price;

    private Long createTime;

    private Integer number;

    private Long orderId;

    public OrderSelleEntrustSucceedEvent(Long createTime, Integer number, Long orderId, Long price) {
        this.createTime = createTime;
        this.number = number;
        this.orderId = orderId;
        this.price = price;
    }

    public OrderSelleEntrustSucceedEvent() {
    }

}
