package com.nd.cqrs.sample;

import com.nd.cqrs.domain.Event;

public class GoodsAddEvent extends Event {

    private String name;

    private long id;

    private int count;

    public GoodsAddEvent() {
        super();
    }

    public GoodsAddEvent(long id, String name, int count) {
        this.id = id;
        this.count = count;
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

    public int getCount() {
        return count;
    }

    public void setCount(int count) {
        this.count = count;
    }

}