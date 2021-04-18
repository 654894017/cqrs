package com.damon.cqrs.sample;

import com.damon.cqrs.domain.Command;

public class GoodsStockAddCommand extends Command{

    public GoodsStockAddCommand(long commandId, long aggregateId) {
        super(commandId, aggregateId);
    }

    private int number;

    public int getNumber() {
        return number;
    }

    public void setNumber(int number) {
        this.number = number;
    }
    
    
}
