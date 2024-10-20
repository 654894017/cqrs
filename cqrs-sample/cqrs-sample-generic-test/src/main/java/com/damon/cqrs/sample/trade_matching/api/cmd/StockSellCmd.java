package com.damon.cqrs.sample.trade_matching.api.cmd;

import com.damon.cqrs.domain.Command;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class StockSellCmd extends Command {
    private Long stockId;
    private Long price;
    private Long orderId;
    private Integer number;
    /**
     * 1 limit order  0 market order
     */
    private int type;

    public StockSellCmd(Long commandId, Long stockId, int type) {
        super(commandId, stockId);
        this.stockId = stockId;
        this.type = type;

    }

    public boolean isLimitOrder() {
        return type == 1;
    }

    public boolean isMarketOrder() {
        return type == 0;
    }
}
