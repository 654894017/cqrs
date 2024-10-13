package com.damon.cqrs.sample.trade_matching.cmd;

import com.damon.cqrs.domain.Command;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class StockGetCmd extends Command {
    private Long stockId;

    public StockGetCmd(Long commandId, Long stockId) {
        super(commandId, stockId);
        this.stockId = stockId;

    }
}
