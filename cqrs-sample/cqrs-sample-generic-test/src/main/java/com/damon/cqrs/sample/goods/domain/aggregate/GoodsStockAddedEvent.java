package com.damon.cqrs.sample.goods.domain.aggregate;

import com.damon.cqrs.domain.Event;

public class GoodsStockAddedEvent extends Event {

    /**
     *
     */
    private static final long serialVersionUID = -7551341932379515484L;
    private int number;

    public GoodsStockAddedEvent() {
        super();
    }


    /**
     * @param number
     */
    public GoodsStockAddedEvent(int number) {
        this.number = number;
    }

    public int getNumber() {
        return number;
    }

    public void setNumber(int number) {
        this.number = number;
    }


}