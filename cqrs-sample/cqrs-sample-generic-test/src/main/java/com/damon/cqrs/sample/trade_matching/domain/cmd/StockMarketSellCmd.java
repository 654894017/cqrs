package com.damon.cqrs.sample.trade_matching.domain.cmd;

import com.damon.cqrs.domain.Command;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class StockMarketSellCmd extends Command {
    private Long stockId;
    private Long orderId;
    private Integer number;
    /**
     * 1 最优5档成交剩余撤销 0 最优5档成交剩余转限价单
     */
    private int entrustmentType;

    public StockMarketSellCmd(Long commandId, Long stockId) {
        super(commandId, stockId);
        this.stockId = stockId;
    }


}
