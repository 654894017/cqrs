package com.damon.cqrs.sample.trade_matching.domain.cmd;

import com.damon.cqrs.domain.Command;

public class CallAuctionCmd extends Command {
    public CallAuctionCmd(Long commandId, Long aggregateId) {
        super(commandId, aggregateId);
    }
}
