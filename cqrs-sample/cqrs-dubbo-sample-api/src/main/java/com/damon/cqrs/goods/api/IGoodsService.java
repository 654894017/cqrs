package com.damon.cqrs.goods.api;

public interface IGoodsService {

    GoodsDO updateStock(GoodsStockAddCommand command);

    GoodsDO saveGoods(GoodsAddCommand command);

}
