package com.damon.cqrs.sample.red_packet;

import com.damon.cqrs.CQRSConfig;
import com.damon.cqrs.sample.Config;
import com.damon.cqrs.sample.red_packet.api.command.RedPacketCreateCommand;
import com.damon.cqrs.sample.red_packet.api.command.RedPacketGrabCommand;
import com.damon.cqrs.sample.red_packet.domain.service.RedPacketDomainServcie;
import com.damon.cqrs.utils.IdWorker;
import org.apache.rocketmq.client.exception.MQClientException;

import java.util.ArrayList;
import java.util.List;
import java.util.Random;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;


public class RedPacketServiceBootstrap {
    public static void main(String[] args) throws InterruptedException, MQClientException {
        CQRSConfig config = Config.init();
        RedPacketDomainServcie redPacketServcie = new RedPacketDomainServcie(config);
        List<Long> ids = new ArrayList<>();
        for (int i = 1; i <= 2000; i++) {
            Long id = IdWorker.getId();
            RedPacketCreateCommand create = new RedPacketCreateCommand(IdWorker.getId(), id);
            create.setMoney(2000d);
            create.setNumber(20000);
            create.setSponsorId(1L);
            redPacketServcie.createRedPackage(create);
            ids.add(id);
        }
        Random random = new Random();
        CountDownLatch latch = new CountDownLatch(4*2000 * 1000);
        int size = ids.size();
        ExecutorService service = Executors.newFixedThreadPool(2000);
        Long startDate = System.currentTimeMillis();
        System.out.println("start");
        for (int i = 0; i < 2000; i++) {
            service.submit(() -> {
                for (int number = 0; number < 300000; number++) {
                    try {
                        int index = random.nextInt(size);
                        Long commandId = IdWorker.getId();
                        Long id = ids.get(index);
                        RedPacketGrabCommand grabCommand = new RedPacketGrabCommand(commandId, id);
                        grabCommand.setUserId(IdWorker.getId());
                        int status = redPacketServcie.grabRedPackage(grabCommand);
                        if (status <= 0) {
                            System.out.println("failed");
                        }
                    } catch (Exception e) {
                        e.printStackTrace();
                    } finally {
                        latch.countDown();
                    }
                }
            });
        }
        latch.await();
        Long endDate = System.currentTimeMillis();
        System.out.println(endDate - startDate);

    }

}

