package com.damon.cqrs.sample;

import com.damon.cqrs.domain.Aggregate;

public class Goods extends Aggregate {

    /**
     * 
     */
    private static final long serialVersionUID = -7591043196387906498L;

    private int number;

    private String name;

    public Goods() {
    }

    public Goods(long id, String name, int number) {
        super(id);
        applyNewEvent(new GoodsAddEvent(id, name, number));
    }

    public int addStock(int number) {
        applyNewEvent(new GoodsStackAddEvent(number));
        return this.number;
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
        return 5;
    }

    

}
