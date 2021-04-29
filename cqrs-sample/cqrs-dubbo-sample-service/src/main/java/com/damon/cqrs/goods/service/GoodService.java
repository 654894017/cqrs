package com.damon.cqrs.goods.service;

import java.util.concurrent.CompletableFuture;

import org.apache.dubbo.config.annotation.DubboService;
import org.springframework.beans.factory.annotation.Autowired;

import com.damon.cqrs.DomainService;
import com.damon.cqrs.EventCommittingService;
import com.damon.cqrs.goods.api.GoodsAddCommand;
import com.damon.cqrs.goods.api.GoodsDO;
import com.damon.cqrs.goods.api.GoodsStockAddCommand;
import com.damon.cqrs.goods.api.IGoodsService;
import com.damon.cqrs.utils.BeanMapper;

/**
 * 商品服务
 * 
 * @author xianping_lu
 *
 */
@DubboService(version = "1.0.0", connections = 600, loadbalance = "consistenthash", retries = 0)
public class GoodService extends DomainService<Goods> implements IGoodsService {

    @Autowired
    public GoodService(EventCommittingService eventCommittingService) {
        super(eventCommittingService);
    }

    @Override
    public GoodsDO saveGoods(GoodsAddCommand command) {
        return process(command, () -> new Goods(command.getAggregateId(), command.getName(), command.getNumber())).thenApply(goods -> {
            return BeanMapper.map(goods, GoodsDO.class);
        }).join();
    }

    @Override
    public GoodsDO updateStock(GoodsStockAddCommand command) {
        return process(command, goods -> goods.addStock(command.getNumber())).thenApply(goods -> {
            return BeanMapper.map(goods, GoodsDO.class);
        }).join();
    }

    @Override
    public CompletableFuture<Goods> getAggregateSnapshoot(long aggregateId, Class<Goods> classes) {
        return CompletableFuture.completedFuture(null);
    }

    @Override
    public CompletableFuture<Boolean> saveAggregateSnapshoot(Goods aggregate) {
        return CompletableFuture.completedFuture(true);
    }

}
