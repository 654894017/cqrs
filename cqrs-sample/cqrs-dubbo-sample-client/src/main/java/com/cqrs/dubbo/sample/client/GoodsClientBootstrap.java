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

    @DubboReference(timeout = 5000, version = "1.0.0", retries = 0, loadbalance = "consistenthash")
    private IGoodsService demoService;

    public static void main(String[] args) {

        SpringApplication.run(GoodsClientBootstrap.class).close();
    }

    @Bean
    public ApplicationRunner runner() {
        CountDownLatch downLatch = new CountDownLatch(400 * 5000);
        Date date = new Date();
        return args -> {
            System.out.println(demoService.saveGoods(new GoodsAddCommand(IdWorker.getId(), 1, "iphone 12", 1000)));
            for (int i = 0; i < 400; i++) {
                new Thread(() -> {
                    for (int count = 0; count < 5000; count++) {
                        try {
                            GoodsDO goods = demoService.updateStock(new GoodsStockAddCommand(IdWorker.getId(), 1, 1));
                            System.out.println(goods);
                        } catch (Exception e) {
                            e.printStackTrace();
                        } finally {
                            // TODO: handle finally clause
                            downLatch.countDown();
                        }
                    }
                }).start();
            }

            System.out.println(new Date() + "-------" + date);
        };
    }

}