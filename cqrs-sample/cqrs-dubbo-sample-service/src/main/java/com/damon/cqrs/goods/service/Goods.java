package com.damon.cqrs.goods.service;

import com.damon.cqrs.domain.Aggregate;
import com.damon.cqrs.goods.api.GoodsAddEvent;
import com.damon.cqrs.goods.api.GoodsStackAddEvent;

public class Goods extends Aggregate {

    private static final long serialVersionUID = -7591043196387906498L;

    private int number;

    private String name;

    public Goods() {
    }

    public Goods(long id, String name, int count) {
        super(id);
        applyNewEvent(new GoodsAddEvent(id, name, count));
    }

    public int addStock(int number) {
        applyNewEvent(new GoodsStackAddEvent(number));
        return 1;
    }

    @SuppressWarnings("unused")
    private void apply(GoodsStackAddEvent event) {
        number += event.getNumber();
    }

    @SuppressWarnings("unused")
    private void apply(GoodsAddEvent event) {
        this.name = event.getName();
        this.number = event.getNumber();
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

    @Override
    public long snapshootCycle() {
        // TODO Auto-generated method stub
        return 5;
    }

}
