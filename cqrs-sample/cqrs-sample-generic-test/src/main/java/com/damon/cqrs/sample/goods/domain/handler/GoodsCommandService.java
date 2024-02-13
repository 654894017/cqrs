package com.damon.cqrs.sample.goods.domain.handler;


import com.damon.cqrs.command.CommandService;
import com.damon.cqrs.config.CqrsConfig;
import com.damon.cqrs.sample.goods.api.GoodsCreateCommand;
import com.damon.cqrs.sample.goods.api.GoodsStockCancelDeductionCommand;
import com.damon.cqrs.sample.goods.api.GoodsStockCommitDeductionCommand;
import com.damon.cqrs.sample.goods.api.GoodsStockTryDeductionCommand;
import com.damon.cqrs.sample.goods.domain.aggregate.Goods;

import java.util.concurrent.CompletableFuture;

public class GoodsCommandService extends CommandService<Goods> implements IGoodsCommandService {

    public GoodsCommandService(CqrsConfig cqrsConfig) {
        super(cqrsConfig);
    }

    @Override
    public CompletableFuture<Goods> createGoodsStock(GoodsCreateCommand command) {
        return super.process(command, () -> new Goods(command.getAggregateId(), command.getName(), command.getNumber()));
    }

    @Override
    public CompletableFuture<Integer> tryDeductionStock(GoodsStockTryDeductionCommand command) {
        return super.process(command, goods -> goods.tryDeductionStock(command.getOrderId(), command.getNumber()));
    }

    @Override
    public CompletableFuture<Integer> commitDeductionStock(GoodsStockCommitDeductionCommand command) {
        return super.process(command, goods -> goods.commitDeductionStock(command.getOrderId()));
    }

    @Override
    public CompletableFuture<Integer> cancelDeductionStock(GoodsStockCancelDeductionCommand command) {
        return super.process(command, goods -> goods.cancelDeductionStock(command.getOrderId()));
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

//    @Override
//    public Goods createAggregateSnapshot(Goods aggregate) {
//        Goods snap = new Goods();
//        snap.setName(aggregate.getName());
//        snap.setNumber(aggregate.getNumber());
//        snap.setId(aggregate.getId());
//        snap.setVersion(aggregate.getVersion());
//        return snap;
//    }
//
//    @Override
//    public long snapshotCycle() {
//        return 5;
//    }


}
