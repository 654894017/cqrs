package com.damon.cqrs.sample.trade_matching.api.event;

import com.damon.cqrs.domain.Event;
import com.damon.cqrs.sample.trade_matching.domain.aggregate.StockBuyOrder;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class OrderBuyEntrustSucceedEvent extends Event {
    private Long price;

    private Long createTime;

    private Integer number;

    private Long orderId;

    public OrderBuyEntrustSucceedEvent(Long createTime, Integer number, Long orderId, Long price) {
        this.createTime = createTime;
        this.number = number;
        this.orderId = orderId;
        this.price = price;
    }

    public OrderBuyEntrustSucceedEvent() {
    }

}
