package com.damon.cqrs.sample.trade_matching.domain.cmd;

import com.damon.cqrs.domain.Command;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class StockOrderMatchCmd extends Command {

    public StockOrderMatchCmd(Long commandId, Long stockId) {
        super(commandId, stockId);
    }
}
