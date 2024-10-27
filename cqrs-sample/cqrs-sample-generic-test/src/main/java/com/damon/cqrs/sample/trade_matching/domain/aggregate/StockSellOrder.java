package com.damon.cqrs.sample.trade_matching.domain.aggregate;

import lombok.Data;

@Data
public class StockSellOrder {
    private Long price;

    private Long createTime;

    private Integer number;
    private Long orderId;

    public StockSellOrder(Long price, Long createTime, Integer number, Long orderId) {
        this.price = price;
        this.createTime = createTime;
        this.number = number;
        this.orderId = orderId;
    }

    public StockSellOrder(Long orderId) {
        this.orderId = orderId;
    }

    public StockSellOrder() {
    }

    public int subtract(Integer number) {
        if (number > this.number) {
            return -2;
        }
        this.number = this.number - number;
        return 0;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;

        StockSellOrder sellOrder = (StockSellOrder) o;
        return orderId.equals(sellOrder.orderId);
    }

    @Override
    public int hashCode() {
        return orderId.hashCode();
    }
}
