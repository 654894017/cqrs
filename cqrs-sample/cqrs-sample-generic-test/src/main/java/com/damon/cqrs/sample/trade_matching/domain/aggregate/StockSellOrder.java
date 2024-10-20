package com.damon.cqrs.sample.trade_matching.domain.aggregate;

import lombok.Data;
import lombok.ToString;

@Data
@ToString
public class StockSellOrder {
    private Long price;

    private Long createTime;

    private Integer number;
    private Long orderId;
    /**
     * 1 limit order  0 market order
     */
    private int type;

    public StockSellOrder(Long price, Long createTime, Integer number, Long orderId, int type) {
        this.price = price;
        this.createTime = createTime;
        this.number = number;
        this.orderId = orderId;
        this.type = type;
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

    public boolean isLimitOrder() {
        return type == 1;
    }

    public boolean isMarketOrder() {
        return type == 0;
    }
}
