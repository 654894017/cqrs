package com.damon.cqrs.sample.goods.domain.handler;


import com.damon.cqrs.sample.goods.api.GoodsCreateCommand;
import com.damon.cqrs.sample.goods.api.GoodsStockCancelDeductionCommand;
import com.damon.cqrs.sample.goods.api.GoodsStockCommitDeductionCommand;
import com.damon.cqrs.sample.goods.api.GoodsStockTryDeductionCommand;
import com.damon.cqrs.sample.goods.domain.aggregate.Goods;

import java.util.concurrent.CompletableFuture;

public interface IGoodsCommandService {

    CompletableFuture<Goods> createGoodsStock(GoodsCreateCommand command);

    CompletableFuture<Integer> tryDeductionStock(GoodsStockTryDeductionCommand command);

    CompletableFuture<Integer> commitDeductionStock(GoodsStockCommitDeductionCommand command);

    CompletableFuture<Integer> cancelDeductionStock(GoodsStockCancelDeductionCommand command);

}
