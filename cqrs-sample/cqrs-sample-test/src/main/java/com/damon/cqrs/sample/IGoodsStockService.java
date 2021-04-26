package com.damon.cqrs.sample;

public interface IGoodsStockService {

    String addStock(long goodsId, int number);
    
    
    String addGoods(long goodsId, String name,int number);

}
