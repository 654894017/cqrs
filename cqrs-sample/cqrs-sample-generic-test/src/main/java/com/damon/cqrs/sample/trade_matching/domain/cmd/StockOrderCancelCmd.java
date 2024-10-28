package com.damon.cqrs.sample.trade_matching.domain.cmd;

import com.damon.cqrs.domain.Command;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class StockOrderCancelCmd extends Command {
    private Long stockId;
    private Long orderId;
    /**
     * 1 买入  0 卖出
     */
    private int type;
    /**
     * 价格
     */
    private Long price;

    public StockOrderCancelCmd(Long commandId, Long stockId, Long orderId, int type) {
        super(commandId, stockId);
        this.stockId = stockId;
        this.type = type;
        this.orderId = orderId;
    }

    public boolean isSellOrder() {
        return type == 0;
    }


    public boolean isBuyOrder() {
        return type == 1;
    }


}
