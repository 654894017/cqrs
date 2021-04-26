package com.damon.cqrs.goods.api;

import com.domain.cqrs.domain.Command;

public class GoodsAddCommand extends Command {

    /**
     * 
     */
    private static final long serialVersionUID = 4656062422498644892L;

    private String name;
    
    private int number;

    public GoodsAddCommand(long commandId, long aggregateId, String name, int number) {
        super(commandId, aggregateId);
        this.name = name;
        this.number = number;

    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public int getNumber() {
        return number;
    }

    public void setNumber(int number) {
        this.number = number;
    }

}
