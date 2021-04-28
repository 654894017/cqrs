package com.damon.cqrs.sample;

import com.domain.cqrs.domain.Event;

public class GoodsStackAddEvent extends Event {

    /**
     * 
     */
    private static final long serialVersionUID = -7551341932379515484L;
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