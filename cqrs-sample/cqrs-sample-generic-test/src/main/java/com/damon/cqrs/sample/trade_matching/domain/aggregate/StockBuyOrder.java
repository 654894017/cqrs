package com.damon.cqrs.sample.trade_matching.domain.aggregate;

import lombok.Data;

import java.util.Objects;

@Data
public class StockBuyOrder {

    private Long price;

    private Long createTime;

    private Integer number;

    private Long orderId;
    /**
     * 1 limit order  0 market order
     */
    private int type;

    public StockBuyOrder(Long price, Long createTime, Integer number, Long orderId) {
        this.price = price;
        this.createTime = createTime;
        this.number = number;
        this.orderId = orderId;
    }

    public StockBuyOrder(Long orderId) {
        this.orderId = orderId;
    }

    public StockBuyOrder() {
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

        StockBuyOrder buyOrder = (StockBuyOrder) o;
        return Objects.equals(orderId, buyOrder.orderId);
    }

    @Override
    public int hashCode() {
        return Objects.hashCode(orderId);
    }
}
