package com.damon.cqrs.sample;

import com.damon.cqrs.domain.Aggregate;

public class Goods extends Aggregate {

    private int count;

    private String name;

    public Goods() {
    }

    public Goods(long id, String name, int count) {
        super(id);
        applyNewEvent(new GoodsAddEvent(id, name, count));
    }

    public void addStock(int number) {
        applyNewEvent(new GoodsStackAddEvent(number));
    }

    @SuppressWarnings("unused")
    private void apply(GoodsStackAddEvent event) {
        count += event.getNumber();
    }

    @SuppressWarnings("unused")
    private void apply(DeductionEvent event) {
        count -= event.getCount();
    }

    @SuppressWarnings("unused")
    private void apply(GoodsAddEvent event) {
        this.name = event.getName();
        this.count = event.getCount();
    }

    public int getCount() {
        return count;
    }

    public String getName() {
        return name;
    }

    public void setCount(int count) {
        this.count = count;
    }

    public void setName(String name) {
        this.name = name;
    }

}
