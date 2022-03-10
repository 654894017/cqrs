package com.damon.cqrs.sample.red_packet;

import com.damon.cqrs.event.EventCommittingService;
import com.damon.cqrs.sample.red_packet.api.command.RedPacketCreateCommand;
import com.damon.cqrs.sample.red_packet.api.command.RedPacketGrabCommand;
import com.damon.cqrs.sample.red_packet.config.CQRSConfig;
import com.damon.cqrs.sample.red_packet.domain.service.RedPacketDomainServcie;
import com.damon.cqrs.utils.IdWorker;
import lombok.extern.slf4j.Slf4j;
import org.apache.rocketmq.client.exception.MQClientException;

import java.util.ArrayList;
import java.util.List;
import java.util.Random;
import java.util.concurrent.CountDownLatch;

@Slf4j
public class RedPacketServiceBootstrap {
    public static void main(String[] args) throws InterruptedException, MQClientException {
        EventCommittingService committingService = CQRSConfig.init();
        RedPacketDomainServcie redPacketServcie = new RedPacketDomainServcie(committingService);
        List<Long> ids = new ArrayList<>();
        for (int i = 0; i < 5000; i++) {
            Long id = IdWorker.getId();
            RedPacketCreateCommand create = new RedPacketCreateCommand(IdWorker.getId(), id + 100000);
            create.setMoney(200d);
            create.setNumber(200);
            create.setSponsorId(1L);
            redPacketServcie.createRedPackage(create);
            ids.add(id + 100000);
        }
        Random random = new Random();
        CountDownLatch latch = new CountDownLatch(400 * 1000);
        int size = ids.size();
        Long startDate = System.currentTimeMillis();
        for (int i = 0; i < 600; i++) {
            new Thread(() -> {
                for (int number = 0; number < 3000; number++) {
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
            }).start();
        }
        latch.await();
        Long endDate = System.currentTimeMillis();
        System.out.println(endDate - startDate);

    }

}

