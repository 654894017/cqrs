package com.damon.cqrs.sample.trade_matching.cmd;

import com.damon.cqrs.domain.Command;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class StockMatchCmd extends Command {

    public StockMatchCmd(Long commandId, Long stockId) {
        super(commandId, stockId);
    }
}
