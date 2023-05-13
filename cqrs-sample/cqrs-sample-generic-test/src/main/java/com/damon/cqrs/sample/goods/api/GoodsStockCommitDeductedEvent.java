package com.damon.cqrs.sample.goods.api;

import com.damon.cqrs.domain.Event;

public class GoodsStockCommitDeductedEvent extends Event {

    /**
     *
     */
    private static final long serialVersionUID = -7551341932379515484L;

    private Long orderId;

    public GoodsStockCommitDeductedEvent() {
        super();
    }

    /**
     * @param number
     */
    public GoodsStockCommitDeductedEvent(Long orderId) {
        this.orderId = orderId;
    }

    public Long getOrderId() {
        return orderId;
    }

    public void setOrderId(Long orderId) {
        this.orderId = orderId;
    }
}