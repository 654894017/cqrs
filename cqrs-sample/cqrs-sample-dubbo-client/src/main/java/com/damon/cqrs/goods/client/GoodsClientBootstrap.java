package com.damon.cqrs.goods.client;

import com.damon.cqrs.goods.api.GoodsCreateCommand;
import com.damon.cqrs.goods.api.GoodsStockAddCommand;
import com.damon.cqrs.goods.api.IGoodsService;
import com.damon.cqrs.utils.EventConflictRetryUtils;
import com.damon.cqrs.utils.IdWorker;
import org.apache.dubbo.config.annotation.DubboReference;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.EnableAutoConfiguration;

import javax.annotation.PostConstruct;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Random;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CountDownLatch;

@EnableAutoConfiguration
public class GoodsClientBootstrap {

    @DubboReference(retries = 0, loadbalance = "consistenthash")
    private IGoodsService goodsService;

    public static void main(String[] args) {
        SpringApplication.run(GoodsClientBootstrap.class);
    }

    @SuppressWarnings("unused")
    @PostConstruct
    public void test() throws InterruptedException {
        CountDownLatch downLatch = new CountDownLatch(1 * 600 * 1000);
        List<Long> ids = new ArrayList<>();
        for (int i = 0; i < 10000; i++) {
            System.out.println(goodsService.createGoods(new GoodsCreateCommand(IdWorker.getId(), i + 1, "iphone " + i, 1000)).join());
            ids.add((long) (i + 1));
        }
        int size = ids.size();
        Random random = new Random();
        Date date = new Date();
        for (int i = 0; i < 600; i++) {
            new Thread(() -> {
                for (int count = 0; count < 1000; count++) {
                    try {
                        int index = random.nextInt(size);
                        GoodsStockAddCommand command = new GoodsStockAddCommand(IdWorker.getId(), ids.get(index), 1);
                        CompletableFuture<Integer> future = EventConflictRetryUtils.invoke(command, () -> goodsService.updateGoodsStock(command));
                        future.join();
                    } catch (Exception e) {
                        e.printStackTrace();
                    } finally {
                        downLatch.countDown();
                    }
                }
            }).start();
        }
        downLatch.await();
        System.out.println(new Date() + "-------" + date);
    }

}