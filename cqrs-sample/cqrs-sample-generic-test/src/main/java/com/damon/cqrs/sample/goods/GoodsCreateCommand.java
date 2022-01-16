package com.damon.cqrs.sample.goods;

import com.damon.cqrs.domain.Command;

public class GoodsCreateCommand extends Command {

    /**
     *
     */
    private static final long serialVersionUID = -452309907057579164L;
    private String name;
    private int number;

    public GoodsCreateCommand(long commandId, long aggregateId, String name, int number) {
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
