package com.damon.cqrs.sample;

import com.damon.cqrs.domain.Event;

public class AddEvent extends Event {

    private int number;
    private String name;

    /**
     * @param number
     */
    public AddEvent(int number,String name) {
        this.number = number;
        this.name=name;
    }

    public int getNumber() {
        return number;
    }

    public void setNumber(int number) {
        this.number = number;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }
    

    
}