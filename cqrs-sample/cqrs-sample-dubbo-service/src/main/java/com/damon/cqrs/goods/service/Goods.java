package com.damon.cqrs.goods.service;

import com.damon.cqrs.domain.AggregateRoot;
import com.damon.cqrs.goods.api.GoodsCreateSucceededEvent;
import com.damon.cqrs.goods.api.GoodsStockAddSucceededEvent;

public class Goods extends AggregateRoot {

    private static final long serialVersionUID = -7591043196387906498L;

    private int number;

    private String name;

    private Long id;


    public Goods(long id, String name, int count) {
        super(id);
        applyNewEvent(new GoodsCreateSucceededEvent(id, name, count));
    }

    public int addStock(int number) {
        applyNewEvent(new GoodsStockAddSucceededEvent(number));
        return 1;
    }

    @SuppressWarnings("unused")
    private void apply(GoodsStockAddSucceededEvent event) {
        number += event.getNumber();
    }

    @SuppressWarnings("unused")
    private void apply(GoodsCreateSucceededEvent event) {
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
    public Long getId() {
        return id;
    }

    @Override
    public void setId(Long id) {
        this.id = id;
    }

//    @Override
//    public long createSnapshotCycle() {
//        // TODO Auto-generated method stub
//        return -1;
//    }
//
//
//    @Override
//    public Goods createSnapshot() {
//        // TODO Auto-generated method stub
//        return CglibUtil.copy(this, Goods.class);
//    }

}
