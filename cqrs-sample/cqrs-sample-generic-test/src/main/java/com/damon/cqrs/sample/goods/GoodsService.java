package com.damon.cqrs.sample.goods;


import com.damon.cqrs.AbstractDomainService;
import com.damon.cqrs.event.EventCommittingService;
import com.damon.cqrs.sample.CQRSConfig;
import com.damon.cqrs.utils.IdWorker;
import org.apache.rocketmq.client.exception.MQClientException;

import java.util.*;
import java.util.concurrent.*;

public class GoodsService extends AbstractDomainService<Goods> {

    public GoodsService(EventCommittingService eventCommittingService) {
        super(eventCommittingService);
    }


    public static void main(String[] args) throws InterruptedException, MQClientException {
        EventCommittingService committingService = CQRSConfig.init();
        GoodsService goodsStockService = new GoodsService(committingService);
        List<Long> ids = new ArrayList<>();
        for (int i = 1; i <= 4; i++) {
            Map<String,Object> shardingParms = new HashMap<>();
            shardingParms.put("a1","a"+i);
            GoodsCreateCommand command1 = new GoodsCreateCommand(IdWorker.getId(), i, "iphone 6 plus " + i, 1000);
            command1.setShardingParams(shardingParms);
            System.out.println(goodsStockService.process(command1, () -> new Goods(command1.getAggregateId(), command1.getName(), command1.getNumber())).join());
            ids.add((long) (i));
        }
        int size = ids.size();
        CountDownLatch latch = new CountDownLatch(4 * 2000 * 1000);
        Date startDate = new Date();
        System.out.println(new Date());
        ExecutorService service = Executors.newFixedThreadPool(800);
        for (int i = 0; i < 1; i++) {
            service.submit(() -> {
                for (int count = 0; count < 1000000; count++) {
                    int index = ThreadLocalRandom.current().nextInt(size);
                    GoodsStockAddCommand command = new GoodsStockAddCommand(IdWorker.getId(), ids.get(index));
                    Map<String,Object> shardingParms = new HashMap<>();
                    shardingParms.put("a1","a"+ids.get(index));
                    command.setShardingParams(shardingParms);
                    CompletableFuture<Integer> future = goodsStockService.process(command, goods -> goods.addStock(1), 3);
                    try {
                        future.join();
                    } catch (Exception e) {
                        e.printStackTrace();
                    } finally {
                        latch.countDown();
                    }
                }
            });
        }
        latch.await();
        System.out.println(startDate);
        System.out.println(new Date());
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
