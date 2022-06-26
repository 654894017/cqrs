package com.damon.cqrs.goods.service;

import java.util.concurrent.CompletableFuture;

import org.apache.dubbo.config.annotation.DubboService;
import org.springframework.beans.factory.annotation.Autowired;

import com.damon.cqrs.AbstractDomainService;
import com.damon.cqrs.CQRSConfig;
import com.damon.cqrs.goods.api.GoodsCreateCommand;
import com.damon.cqrs.goods.api.GoodsDTO;
import com.damon.cqrs.goods.api.GoodsStockAddCommand;
import com.damon.cqrs.goods.api.IGoodsService;
import com.damon.cqrs.utils.BeanMapper;

/**
 * 商品服务
 *
 * @author xianping_lu
 */
@DubboService(loadbalance = "consistenthash", retries = 0, timeout = 50000)
public class GoodService extends AbstractDomainService<Goods> implements IGoodsService {

    @Autowired
    public GoodService(CQRSConfig config) {
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
