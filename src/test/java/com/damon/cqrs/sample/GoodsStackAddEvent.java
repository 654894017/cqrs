package com.damon.cqrs.sample;

import com.damon.cqrs.domain.Event;

public class GoodsStackAddEvent extends Event {

    private int number;

    /**
     * @param number
     */
    public GoodsStackAddEvent(int number) {
        this.number = number;
    }

    public int getNumber() {
        return number;
    }

    public void setNumber(int number) {
        this.number = number;
    }

    
}