package com.damon.cqrs.goods.service;

import java.util.concurrent.CompletableFuture;

import org.apache.dubbo.config.annotation.DubboService;
import org.springframework.beans.factory.annotation.Autowired;

import com.damon.cqrs.AbstractCommandHandler;
import com.damon.cqrs.CQRSConfig;
import com.damon.cqrs.goods.api.GoodsCreateCommand;
import com.damon.cqrs.goods.api.GoodsDTO;
import com.damon.cqrs.goods.api.GoodsStockAddCommand;
import com.damon.cqrs.goods.api.ICommandHandler;
import com.damon.cqrs.utils.BeanMapper;

/**
 * 商品服务
 *
 * @author xianping_lu
 */
@DubboService(loadbalance = "consistenthash", retries = 0, timeout = 50000)
public class GoodCommandHandler extends AbstractCommandHandler<Goods> implements ICommandHandler {

    @Autowired
    public GoodCommandHandler(CQRSConfig config) {
        super(config);
    }

    @Override
    public CompletableFuture<GoodsDTO> createGoods(GoodsCreateCommand command) {
        return process(command, () ->
                new Goods(command.getAggregateId(), command.getName(), command.getNumber())
        ).thenApply(goods ->
                BeanMapper.map(goods, GoodsDTO.class)
        );
    }

    @Override
    public CompletableFuture<Integer> updateGoodsStock(GoodsStockAddCommand command) {
        return process(command, goods -> {
            return goods.addStock(command.getNumber());
        });
    }


}
