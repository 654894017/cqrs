package com.damon.cqrs.sample.goods;

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
        applyNewEvent(new GoodsCreatedEvent(id, name, number));
    }

    public int addStock(int number) {
        applyNewEvent(new GoodsStockAddedEvent(number));
        return this.number;
    }

    public int changeGoodsName(long id, String name, int version){
        if(getVersion() == version){
            applyNewEvent(new GoodsNameChangedEvent(name));
            return 0;
        }
        return 1;
    }

    @SuppressWarnings("unused")
    private void apply(GoodsStockAddedEvent event) {
        number += event.getNumber();
    }

    private void apply(GoodsNameChangedEvent event){
        this.name = event.getName();
    }


    @SuppressWarnings("unused")
    private void apply(GoodsCreatedEvent event) {
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
    public long createSnapshootCycle() {
        return 5;
    }


}
