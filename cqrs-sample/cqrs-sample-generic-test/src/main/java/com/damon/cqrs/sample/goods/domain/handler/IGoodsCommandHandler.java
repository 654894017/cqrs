package com.damon.cqrs.sample.goods.domain.handler;


import java.util.concurrent.CompletableFuture;

import com.damon.cqrs.sample.goods.domain.aggregate.Goods;
import com.damon.cqrs.sample.goods.domain.aggregate.GoodsCreateCommand;
import com.damon.cqrs.sample.goods.domain.aggregate.GoodsStockAddCommand;

public interface IGoodsCommandHandler {

    CompletableFuture<Goods> createGoodsStock(GoodsCreateCommand command);
    
    
    CompletableFuture<Integer> addGoodsStock(GoodsStockAddCommand command);
    

}
