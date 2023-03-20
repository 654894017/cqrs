package com.damon.cqrs.sample.goods.domain.handler;


import com.damon.cqrs.sample.goods.api.GoodsCreateCommand;
import com.damon.cqrs.sample.goods.api.GoodsStockAddCommand;
import com.damon.cqrs.sample.goods.domain.aggregate.Goods;

import java.util.concurrent.CompletableFuture;

public interface IGoodsCommandHandler {

    CompletableFuture<Void> createGoodsStock(GoodsCreateCommand command);


    CompletableFuture<Integer> addGoodsStock(GoodsStockAddCommand command);


}
