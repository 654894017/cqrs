package com.damon.cqrs.goods.api;

public interface IGoodsService {
    GoodsDO createGoods(GoodsCreateCommand command);

    int updateGoodsStock(GoodsStockAddCommand command);
}
