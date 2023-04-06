package com.damon.cqrs.sample.goods.domain.handler;


import com.damon.cqrs.CqrsConfig;
import com.damon.cqrs.CommandService;
import com.damon.cqrs.sample.goods.api.GoodsCreateCommand;
import com.damon.cqrs.sample.goods.api.GoodsStockAddCommand;
import com.damon.cqrs.sample.goods.domain.aggregate.Goods;

import java.util.concurrent.CompletableFuture;

public class GoodsCommandService extends CommandService<Goods> implements IGoodsCommandHandler {

    public GoodsCommandService(CqrsConfig cqrsConfig) {
        super(cqrsConfig);
    }

    @Override
    public CompletableFuture<Void> createGoodsStock(GoodsCreateCommand command) {
        return super.process(command, () -> new Goods(command.getAggregateId(), command.getName(), command.getNumber()));
    }


    @Override
    public CompletableFuture<Integer> addGoodsStock(GoodsStockAddCommand command) {
        return super.process(command, goods -> goods.addStock(1));
    }

    @Override
    public CompletableFuture<Goods> getAggregateSnapshot(long aggregateId, Class<Goods> classes) {
        return CompletableFuture.completedFuture(null);
    }

    @Override
    public CompletableFuture<Boolean> saveAggregateSnapshot(Goods goods) {
        //System.out.println(goods.getId() + ":" + goods.getNumber() + ":" + goods.getName() + ":" + goods.getVersion());
        return CompletableFuture.completedFuture(true);
    }

     @Override
    public Goods createAggregateSnapshot(Goods aggregate) {
         Goods snap = new Goods();
         snap.setName(aggregate.getName());
         snap.setNumber(aggregate.getNumber());
         snap.setId(aggregate.getId());
         snap.setVersion(aggregate.getVersion());
        return snap;
    }

    @Override
    public long snapshotCycle() {
        net.sf.cglib.beans.BeanCopier cop;
        return 5;
    }


}
