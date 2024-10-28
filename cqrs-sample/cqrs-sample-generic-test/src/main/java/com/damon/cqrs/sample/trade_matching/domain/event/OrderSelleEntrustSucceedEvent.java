package com.damon.cqrs.sample.trade_matching.domain.event;

import com.damon.cqrs.domain.Event;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class OrderSelleEntrustSucceedEvent extends Event {

    private Long price;

    private Long createTime;

    private Integer number;

    private Long orderId;

    public OrderSelleEntrustSucceedEvent(Long orderId, Long price, Long createTime, Integer number) {
        this.createTime = createTime;
        this.number = number;
        this.orderId = orderId;
        this.price = price;
    }

    public OrderSelleEntrustSucceedEvent() {
    }

}
