package com.cqrs.dubbo.sample.client;

import java.util.Date;
import java.util.concurrent.CountDownLatch;

import org.apache.dubbo.config.annotation.DubboReference;
import org.springframework.boot.ApplicationRunner;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.EnableAutoConfiguration;
import org.springframework.context.annotation.Bean;

import com.damon.cqrs.goods.api.GoodsAddCommand;
import com.damon.cqrs.goods.api.GoodsDO;
import com.damon.cqrs.goods.api.GoodsStockAddCommand;
import com.damon.cqrs.goods.api.IGoodsService;

@EnableAutoConfiguration
public class GoodsClientBootstrap {

    @DubboReference(url = "dubbo://172.18.133.142:12345", timeout = 50000, version = "1.0.0", retries = 0)
    private IGoodsService demoService;

    public static void main(String[] args) {

        SpringApplication.run(GoodsClientBootstrap.class).close();

    }

    @Bean
    public ApplicationRunner runner() {
        CountDownLatch downLatch = new CountDownLatch(2000000);
        Date date = new Date();
        return args -> {
            System.out.println(demoService.saveGoods(new GoodsAddCommand(IdWorker.getId(), 1, "iphone 12", 1000)));
            for (int i = 0; i < 5; i++) {
                new Thread(() -> {
                    for (int count = 0; count < 5000; count++) {
                        GoodsDO goods = demoService.updateStock(new GoodsStockAddCommand(IdWorker.getId(), 1, 1));
                        System.out.println(goods);
                        downLatch.countDown();
                    }
                }).start();
            }
            downLatch.await();
            System.out.println(new Date() + "-------" + date);
        };
    }

}