package com.damon.cqrs.sample.trade_matching.domain.cmd;

import com.damon.cqrs.domain.Command;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class StockBuyCmd extends Command {
    private Long stockId;
    private Long price;
    private Long orderId;
    private Integer number;

    public StockBuyCmd(Long commandId, Long stockId) {
        super(commandId, stockId);
        this.stockId = stockId;
    }

}
