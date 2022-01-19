package com.damon.cqrs.sample.train.command;


import com.damon.cqrs.domain.Command;

import java.util.List;

public class TrainCreateCommand extends Command {

    private List<Integer> s2s;

    private Integer seatCount;

    /**
     * @param commandId
     * @param aggregateId
     */
    public TrainCreateCommand(long commandId, long aggregateId) {
        super(commandId, aggregateId);
    }

    public List<Integer> getS2s() {
        return s2s;
    }

    public void setS2s(List<Integer> s2s) {
        this.s2s = s2s;
    }

    public Integer getSeatCount() {
        return seatCount;
    }

    public void setSeatCount(Integer seatCount) {
        this.seatCount = seatCount;
    }
}

