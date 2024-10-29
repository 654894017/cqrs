package com.damon.cqrs.sample.trade_matching.domain.event;

import com.damon.cqrs.domain.Event;
import lombok.Data;

@Data
public class CallAuctionSucceedEvent extends Event {

    private Long price;

    public CallAuctionSucceedEvent(Long price) {
        this.price = price;
    }

    public CallAuctionSucceedEvent() {
    }
}
