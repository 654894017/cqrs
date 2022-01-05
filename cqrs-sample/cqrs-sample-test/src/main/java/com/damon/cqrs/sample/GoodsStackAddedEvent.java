package com.damon.cqrs.sample;

import com.damon.cqrs.domain.Event;

public class GoodsStackAddedEvent extends Event {

    /**
     *
     */
    private static final long serialVersionUID = -7551341932379515484L;
    private int number;

    public GoodsStackAddedEvent() {
        super();
    }


    /**
     * @param number
     */
    public GoodsStackAddedEvent(int number) {
        this.number = number;
    }

    public int getNumber() {
        return number;
    }

    public void setNumber(int number) {
        this.number = number;
    }


}