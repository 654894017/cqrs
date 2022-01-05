package com.damon.cqrs.goods.api;

import com.damon.cqrs.domain.Event;

public class GoodsCreatedEvent extends Event {

    /**
     *
     */
    private static final long serialVersionUID = -930408688795798265L;

    private String name;

    private long id;

    private int number;

    public GoodsCreatedEvent() {
        super();
    }

    public GoodsCreatedEvent(long id, String name, int number) {
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