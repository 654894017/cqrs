package com.damon.cqrs.sample.goods;

import com.damon.cqrs.domain.Event;

public class GoodsNameChangedEvent extends Event {

    /**
     *
     */
    private static final long serialVersionUID = -7551341932379515484L;
    private String name;

    public GoodsNameChangedEvent() {
        super();
    }


    public GoodsNameChangedEvent(String name) {
        this();
        this.name = name;
    }

    public String getName() {
        return name;
    }

}