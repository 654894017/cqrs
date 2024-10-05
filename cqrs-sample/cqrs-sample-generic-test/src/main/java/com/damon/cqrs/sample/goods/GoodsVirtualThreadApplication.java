package com.damon.cqrs.sample.goods;

import com.damon.cqrs.config.CqrsConfig;
import com.damon.cqrs.sample.TestConfig;
import com.damon.cqrs.sample.goods.api.GoodsCreateCommand;
import com.damon.cqrs.sample.goods.api.GoodsStockTryDeductionCommand;
import com.damon.cqrs.sample.goods.domain.handler.GoodsCommandService;
import com.damon.cqrs.sample.goods.domain.handler.IGoodsCommandService;
import com.damon.cqrs.utils.IdWorker;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.*;

/**
 * 基于jdk 19 虚拟线程测试
 *
 * @author xianpinglu
 */
public class GoodsVirtualThreadApplication {
    private static final int goodsCount = 1000;

    private static final int threadNumber = 4000;

    @SuppressWarnings("preview")
    private static final ExecutorService service = Executors.newVirtualThreadPerTaskExecutor();

    private static final int exeCount = 2000;
    private static final int runTotalCount = threadNumber * exeCount;

    public static void main(String[] args) throws Exception {
        CqrsConfig cqrsConfig = TestConfig.init();
        IGoodsCommandService handler = new GoodsCommandService(cqrsConfig);
        List<Long> goodsIds = initGoods(handler);
        int size = goodsIds.size();
        CountDownLatch latch = new CountDownLatch(runTotalCount);
        long from = System.currentTimeMillis();
        for (int i = 0; i < threadNumber; i++) {
            service.submit(() -> {
                for (int count = 0; count < exeCount; count++) {
                    int index = ThreadLocalRandom.current().nextInt(size);
                    GoodsStockTryDeductionCommand cmd = new GoodsStockTryDeductionCommand(IdWorker.getId(), goodsIds.get(index));
                    cmd.setNumber(1);
                    cmd.setOrderId(IdWorker.getId());
                    CompletableFuture<Integer> future = handler.tryDeductionStock(cmd);
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
        long tps = runTotalCount / (from - System.currentTimeMillis() / 1000);
        System.out.println("tps:" + tps);
    }

    private static List<Long> initGoods(IGoodsCommandService handler) {
        List<Long> ids = new ArrayList<>();
        for (int i = 1; i <= goodsCount; i++) {
            Map<String, Object> shardingParms = new HashMap<>();
            shardingParms.put("a1", "a" + i);
            GoodsCreateCommand command1 = new GoodsCreateCommand(IdWorker.getId(), i, "iphone " + i, 1000000000);
            handler.createGoodsStock(command1).join();
            ids.add((long) (i));
        }
        System.out.println("初始化商品成功, 个数:" + goodsCount);
        return ids;
    }

    private static long calculateTimeConsumption(long from, long to) {
        return to - from;
    }

}
