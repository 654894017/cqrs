package com.damon.cqrs.sample.goods;


import com.damon.cqrs.AbstractDomainService;
import com.damon.cqrs.event.EventCommittingService;
import com.damon.cqrs.sample.CQRSConfig;
import com.damon.cqrs.utils.IdWorker;
import org.apache.rocketmq.client.exception.MQClientException;

import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Random;
import java.util.concurrent.*;

public class GoodsService extends AbstractDomainService<Goods> {

    public GoodsService(EventCommittingService eventCommittingService) {
        super(eventCommittingService);
    }


    public static void main(String[] args) throws InterruptedException, MQClientException {
        EventCommittingService committingService = CQRSConfig.init();
        GoodsService goodsStockService = new GoodsService(committingService);
        List<Long> ids = new ArrayList<>();
        for (int i = 1; i <= 5000; i++) {
            GoodsCreateCommand command1 = new GoodsCreateCommand(IdWorker.getId(), i, "iphone 6 plus " + i, 1000);
            System.out.println(goodsStockService.process(command1, () -> new Goods(command1.getAggregateId(), command1.getName(), command1.getNumber())).join());
            ids.add((long) (i));
        }
        int size = ids.size();
        CountDownLatch latch = new CountDownLatch(1 * 2000 * 1000);
        Date startDate = new Date();
        System.out.println(new Date());
        ExecutorService service = Executors.newFixedThreadPool(100);
        for (int i = 0; i < 100; i++) {
            service.submit(() -> {
                for (int count = 0; count < 1000000; count++) {
                    int index = ThreadLocalRandom.current().nextInt(size);
                    GoodsStockAddCommand command = new GoodsStockAddCommand(IdWorker.getId(), ids.get(index));
                    CompletableFuture<Integer> future = goodsStockService.process(command, goods -> goods.addStock(1), 3);
                    try {
                        future.join();
                        //System.out.println(status);
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
    public CompletableFuture<Goods> getAggregateSnapshoot(long aggregateId, Class<Goods> classes) {
        return CompletableFuture.supplyAsync(() -> {
            return null;
        });

    }

    @Override
    public CompletableFuture<Boolean> saveAggregateSnapshoot(Goods goods) {
        System.out.println(goods.getId() + ":" + goods.getNumber() + ":" + goods.getName() + ":" + goods.getVersion());
        return CompletableFuture.completedFuture(true);
    }

}
