package com.damon.cqrs.goods.api;

import com.domain.cqrs.domain.Event;

public class GoodsAddEvent extends Event {

    /**
     * 
     */
    private static final long serialVersionUID = -930408688795798265L;

    private String name;

    private long id;

    private int number;

    public GoodsAddEvent() {
        super();
    }

    public GoodsAddEvent(long id, String name, int number) {
        this.id = id;
        this.number = number;
        this.name = name;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public long getId() {
        return id;
    }

    public void setId(long id) {
        this.id = id;
    }

    public int getNumber() {
        return number;
    }

    public void setNumber(int number) {
        this.number = number;
    }

}