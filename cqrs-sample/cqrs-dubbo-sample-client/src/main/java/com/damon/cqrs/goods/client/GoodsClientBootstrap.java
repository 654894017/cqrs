package com.damon.cqrs.goods.client;

import java.util.Date;
import java.util.concurrent.CountDownLatch;

import javax.annotation.PostConstruct;

import org.apache.dubbo.config.annotation.DubboReference;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.EnableAutoConfiguration;

import com.damon.cqrs.goods.api.GoodsAddCommand;
import com.damon.cqrs.goods.api.GoodsDO;
import com.damon.cqrs.goods.api.GoodsStockAddCommand;
import com.damon.cqrs.goods.api.IGoodsService;

@EnableAutoConfiguration
public class GoodsClientBootstrap {

    @DubboReference(timeout = 220000, version = "1.0.0", retries = 0, loadbalance = "consistenthash")
    private IGoodsService goodsService;

    public static void main(String[] args) {
        SpringApplication.run(GoodsClientBootstrap.class);
    }

    @PostConstruct
    public void test() throws InterruptedException {
        CountDownLatch downLatch = new CountDownLatch(2*300 * 3000);
        System.out.println(goodsService.saveGoods(new GoodsAddCommand(IdWorker.getId(), 2, "iphone 12", 1000)));
        System.out.println(goodsService.saveGoods(new GoodsAddCommand(IdWorker.getId(), 1, "iphone 12", 1000)));
        Date date = new Date();
        for (int i = 0; i < 300; i++) {
            new Thread(() -> {
                for (int count = 0; count < 3000; count++) {
                    try {
                        GoodsDO goods = goodsService.updateStock(new GoodsStockAddCommand(IdWorker.getId(), 2, 1));
                        //System.out.println(goods);
                    } catch (Exception e) {
                        e.printStackTrace();
                    } finally {
                        downLatch.countDown();
                    }
                }
            }).start();
        }
        for (int i = 0; i < 300; i++) {
            new Thread(() -> {
                for (int count = 0; count < 3000; count++) {
                    try {
                        GoodsDO goods = goodsService.updateStock(new GoodsStockAddCommand(IdWorker.getId(), 1, 1));
                        //System.out.println(goods);
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