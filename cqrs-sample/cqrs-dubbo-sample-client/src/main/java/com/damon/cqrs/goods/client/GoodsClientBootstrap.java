package com.damon.cqrs.goods.client;

import com.damon.cqrs.goods.api.GoodsAddCommand;
import com.damon.cqrs.goods.api.GoodsDO;
import com.damon.cqrs.goods.api.GoodsStockAddCommand;
import com.damon.cqrs.goods.api.IGoodsService;
import com.damon.cqrs.utils.EventConflictRetryUtils;
import com.damon.cqrs.utils.IdWorker;
import org.apache.dubbo.config.annotation.DubboReference;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.EnableAutoConfiguration;

import javax.annotation.PostConstruct;
import java.util.Date;
import java.util.concurrent.CountDownLatch;

@EnableAutoConfiguration
public class GoodsClientBootstrap {

    @DubboReference(timeout = 220000, version = "1.0.0", retries = 0, loadbalance = "consistenthash")
    private IGoodsService goodsService;

    public static void main(String[] args) {
        SpringApplication.run(GoodsClientBootstrap.class);
    }

    @SuppressWarnings("unused")
    @PostConstruct
    public void test() throws InterruptedException {
        CountDownLatch downLatch = new CountDownLatch(2 * 300 * 3000);
        System.out.println(goodsService.createGoods(new GoodsAddCommand(IdWorker.getId(), 2, "iphone 12", 1000)));
        System.out.println(goodsService.createGoods(new GoodsAddCommand(IdWorker.getId(), 1, "iphone 13", 1000)));
        Date date = new Date();
        for (int i = 0; i < 1; i++) {
            new Thread(() -> {
                for (int count = 0; count < 3000; count++) {
                    try {
                        GoodsStockAddCommand command = new GoodsStockAddCommand(IdWorker.getId(), 2, 1);
                        int i2 = EventConflictRetryUtils.invoke(command, () -> goodsService.updateGoodsStock(command));
                    } catch (Exception e) {
                        e.printStackTrace();
                    } finally {
                        downLatch.countDown();
                    }
                }
            }).start();
        }
//        for (int i = 0; i < 300; i++) {
//            new Thread(() -> {
//                for (int count = 0; count < 3000; count++) {
//                    try {
//                        GoodsStockAddCommand command = new GoodsStockAddCommand(IdWorker.getId(), 1, 1);
//                        int i2  = EventConflictRetryUtils.invoke(command, () -> goodsService.updateGoodsStock(command));
//                    } catch (Exception e) {
//                        e.printStackTrace();
//                    } finally {
//                        downLatch.countDown();
//                    }
//                }
//            }).start();
//        }
        downLatch.await();
        System.out.println(new Date() + "-------" + date);
    }

}