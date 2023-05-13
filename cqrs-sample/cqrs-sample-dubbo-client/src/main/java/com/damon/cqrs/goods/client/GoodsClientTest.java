package com.damon.cqrs.goods.client;

import com.damon.cqrs.goods.api.GoodsCreateCommand;
import com.damon.cqrs.goods.api.GoodsStockAddCommand;
import com.damon.cqrs.goods.api.IGoodsCommandService;
import com.damon.cqrs.utils.AggregateConflictRetryUtils;
import com.damon.cqrs.utils.IdWorker;
import org.apache.dubbo.config.ProtocolConfig;
import org.apache.dubbo.config.ReferenceConfig;
import org.apache.dubbo.config.RegistryConfig;
import org.apache.dubbo.config.bootstrap.DubboBootstrap;

import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Random;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CountDownLatch;

public class GoodsClientTest {

    public static void main(String[] args) throws InterruptedException {
        ReferenceConfig<IGoodsCommandService> reference = new ReferenceConfig<>();
        reference.setInterface(IGoodsCommandService.class);
        reference.setTimeout(50000);

        ProtocolConfig protocolConfig = new ProtocolConfig("dubbo", -1);
        protocolConfig.setSerialization("kryo");
        DubboBootstrap.getInstance()
                .application("first-dubbo-consumer")
                .registry(new RegistryConfig("zookeeper://127.0.0.1:2181"))
                .protocol(protocolConfig)
                .reference(reference);

        IGoodsCommandService goodsService = reference.get();

        CountDownLatch downLatch = new CountDownLatch(1 * 1 * 1000);
        List<Long> ids = new ArrayList<>();
        for (int i = 0; i < 10; i++) {
            System.out.println(goodsService.createGoods(new GoodsCreateCommand(IdWorker.getId(), i + 1, "iphone " + i, 1000)).join());
            ids.add((long) (i + 1));
        }
        int size = ids.size();
        Random random = new Random();
        Date date = new Date();
        for (int i = 0; i < 1; i++) {
            new Thread(() -> {
                for (int count = 0; count < 1000; count++) {
                    try {
                        int index = random.nextInt(size);
                        GoodsStockAddCommand command = new GoodsStockAddCommand(IdWorker.getId(), ids.get(index), 1);
                        CompletableFuture<Integer> future = AggregateConflictRetryUtils.invoke(command, () -> goodsService.updateGoodsStock(command));
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
