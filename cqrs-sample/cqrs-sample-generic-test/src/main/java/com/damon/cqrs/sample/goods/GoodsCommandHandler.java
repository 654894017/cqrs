package com.damon.cqrs.sample.goods;


import com.damon.cqrs.AbstractCommandHandler;
import com.damon.cqrs.CQRSConfig;

import java.util.concurrent.CompletableFuture;

public class GoodsCommandHandler extends AbstractCommandHandler<Goods> {

    public GoodsCommandHandler(CQRSConfig config) {
        super(config);
    }

    @Override
    public CompletableFuture<Goods> getAggregateSnapshot(long aggregateId, Class<Goods> classes) {
        return CompletableFuture.completedFuture(null);
    }

    @Override
    public CompletableFuture<Boolean> saveAggregateSnapshot(Goods goods) {
        System.out.println(goods.getId() + ":" + goods.getNumber() + ":" + goods.getName() + ":" + goods.getVersion());
        return CompletableFuture.completedFuture(true);
    }

}
