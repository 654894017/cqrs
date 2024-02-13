package com.damon.cqrs.goods.service;

import cn.hutool.extra.cglib.CglibUtil;
import com.damon.cqrs.command.CommandService;
import com.damon.cqrs.config.CqrsConfig;
import com.damon.cqrs.goods.api.GoodsCreateCommand;
import com.damon.cqrs.goods.api.GoodsDTO;
import com.damon.cqrs.goods.api.GoodsStockAddCommand;
import com.damon.cqrs.goods.api.IGoodsCommandService;

import java.util.concurrent.CompletableFuture;

/**
 * 商品服务
 *
 * @author xianping_lu
 */

public class GoodsCommandService extends CommandService<Goods> implements IGoodsCommandService {

    public GoodsCommandService(CqrsConfig cqrsConfig) {
        super(cqrsConfig);
    }

    @Override
    public CompletableFuture<GoodsDTO> createGoods(GoodsCreateCommand command) {
        return process(command, () ->
                new Goods(command.getAggregateId(), command.getName(), command.getNumber())
        ).thenApply(goods ->
                CglibUtil.copy(goods, GoodsDTO.class)
        );
    }

    @Override
    public CompletableFuture<Integer> updateGoodsStock(GoodsStockAddCommand command) {
        return process(command, goods -> goods.addStock(command.getNumber()));
    }


}
