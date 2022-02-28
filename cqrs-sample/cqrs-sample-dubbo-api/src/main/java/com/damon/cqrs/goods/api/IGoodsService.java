package com.damon.cqrs.goods.api;

import java.util.concurrent.CompletableFuture;

public interface IGoodsService {
    CompletableFuture<GoodsDO> createGoods(GoodsCreateCommand command);

    CompletableFuture<Integer> updateGoodsStock(GoodsStockAddCommand command);
}
