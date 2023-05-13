package com.damon.cqrs.goods.api;

import java.util.concurrent.CompletableFuture;

public interface IGoodsCommandService {
    CompletableFuture<GoodsDTO> createGoods(GoodsCreateCommand command);

    CompletableFuture<Integer> updateGoodsStock(GoodsStockAddCommand command);
}
