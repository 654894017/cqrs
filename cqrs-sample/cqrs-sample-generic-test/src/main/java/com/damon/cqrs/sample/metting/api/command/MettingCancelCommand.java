package com.damon.cqrs.sample.metting.api.command;

import com.damon.cqrs.domain.Command;

public class MettingCancelCommand extends Command {

    private String reserveFlag;

    private Long userId;

    public MettingCancelCommand(Long commandId, Long aggregateId, String reserveFlag, Long userId) {
        super(commandId, aggregateId);
        this.reserveFlag = reserveFlag;
        this.userId = userId;
    }

    public String getReserveFlag() {
        return reserveFlag;
    }


    public Long getUserId() {
        return userId;
    }

}
