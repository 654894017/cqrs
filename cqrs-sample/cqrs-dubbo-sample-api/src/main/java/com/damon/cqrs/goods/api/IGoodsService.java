package com.damon.cqrs.goods.api;

public interface IGoodsService {

    
    GoodsDO saveGoods(GoodsAddCommand command);


    GoodsDO updateStock(GoodsStockAddCommand command);
    
}
