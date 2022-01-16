package com.damon.cqrs.goods.api;

public interface IGoodsService {
    GoodsDO createGoods(GoodsAddCommand command);

    int updateGoodsStock(GoodsStockAddCommand command);
}
