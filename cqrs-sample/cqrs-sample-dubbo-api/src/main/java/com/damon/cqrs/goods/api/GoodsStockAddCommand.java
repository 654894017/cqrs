package com.damon.cqrs.goods.api;

import com.damon.cqrs.domain.Command;

public class GoodsStockAddCommand extends Command {

    /**
     *
     */
    private static final long serialVersionUID = 6170493556439287437L;
    private int number;

    public GoodsStockAddCommand(long commandId, long aggregateId, int number) {
        super(commandId, aggregateId);
        this.number = number;
    }

    public int getNumber() {
        return number;
    }

    public void setNumber(int number) {
        this.number = number;
    }

}
