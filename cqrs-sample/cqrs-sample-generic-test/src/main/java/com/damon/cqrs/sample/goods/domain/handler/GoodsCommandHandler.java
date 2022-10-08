package com.damon.cqrs.sample.goods.domain.handler;


import java.util.concurrent.CompletableFuture;

import com.damon.cqrs.CQRSConfig;
import com.damon.cqrs.CommandHandler;
import com.damon.cqrs.sample.goods.domain.aggregate.Goods;
import com.damon.cqrs.sample.goods.domain.aggregate.GoodsCreateCommand;
import com.damon.cqrs.sample.goods.domain.aggregate.GoodsStockAddCommand;

public class GoodsCommandHandler extends CommandHandler<Goods> implements IGoodsCommandHandler{

    public GoodsCommandHandler(CQRSConfig config) {
        super(config);
    }

    @Override
    public CompletableFuture<Goods> createGoodsStock(GoodsCreateCommand command) {
        return super.process(command, () -> new Goods(command.getAggregateId(), command.getName(), command.getNumber()));
    }
    
    
    @Override
    public CompletableFuture<Integer> addGoodsStock(GoodsStockAddCommand command) {
        return super.process(command, goods -> goods.addStock(1));
    }
    
//    @Override
//    public CompletableFuture<Goods> getAggregateSnapshot(long aggregateId, Class<Goods> classes) {
//        return CompletableFuture.completedFuture(null);
//    }
//
//    @Override
//    public CompletableFuture<Boolean> saveAggregateSnapshot(Goods goods) {
//        System.out.println(goods.getId() + ":" + goods.getNumber() + ":" + goods.getName() + ":" + goods.getVersion());
//        return CompletableFuture.completedFuture(true);
//    }
//
//     @Override
//    public Goods createAggregateSnapshot(Goods aggregate) {
//        return CglibUtil.copy(aggregate, Goods.class);
//    }
//     
//    @Override
//    public long createSnapshotCycle() {
//        return 5;
//    }


}
