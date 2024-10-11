package com.damon.cqrs.sample.trade_matching.model;

import lombok.Data;
import lombok.ToString;

@Data
@ToString
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


    public StockSellOrder() {
    }

    public int deduction(Integer number) {
        if (number > this.number) {
            return -2;
        }
        this.number = this.number - number;
        return 0;
    }
}
