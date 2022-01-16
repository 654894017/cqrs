package com.damon.cqrs.sample.red_packet;

import com.alibaba.fastjson.JSONObject;
import com.damon.cqrs.event.EventCommittingService;
import com.damon.cqrs.sample.red_packet.command.RedPacketCreateCommand;
import com.damon.cqrs.sample.red_packet.command.RedPacketGetCommand;
import com.damon.cqrs.sample.red_packet.command.RedPacketGrabCommand;
import com.damon.cqrs.sample.red_packet.command.RedPacketTypeEnum;
import com.damon.cqrs.sample.red_packet.domain_service.CqrsConfig;
import com.damon.cqrs.sample.red_packet.domain_service.RedPacketDomainServcie;
import com.damon.cqrs.sample.red_packet.dto.WeixinRedPacketDTO;
import com.damon.cqrs.utils.IdWorker;
import org.apache.rocketmq.client.exception.MQClientException;

import java.util.concurrent.CountDownLatch;

public class RedPacketServiceBootstrap {
    public static void main(String[] args) throws InterruptedException, MQClientException {
        EventCommittingService committingService = CqrsConfig.init();
        RedPacketDomainServcie redPacketServcie = new RedPacketDomainServcie(committingService);
        Long id = IdWorker.getId();
        RedPacketCreateCommand create = new RedPacketCreateCommand(IdWorker.getId(), id);
        create.setMoney(1000000L);
        create.setNumber(1000000);
        create.setSponsorId(1L);
        create.setType(RedPacketTypeEnum.AVG);
        redPacketServcie.createRedPackage(create);

        Long startDate = System.currentTimeMillis();
        CountDownLatch latch = new CountDownLatch(1000000);
        for (int i = 0; i < 500; i++) {
            new Thread(() -> {
                for (int number = 0; number < 2000; number++) {
                    RedPacketGrabCommand grabCommand = new RedPacketGrabCommand(IdWorker.getId(), id);
                    grabCommand.setUserId(IdWorker.getId());
                    redPacketServcie.grabRedPackage(grabCommand);
                    latch.countDown();
                }
            }).start();
        }
        latch.await();
        Long endDate = System.currentTimeMillis();
        System.out.println(endDate - startDate);
        RedPacketGetCommand getCommand = new RedPacketGetCommand(IdWorker.getId(), id);
        WeixinRedPacketDTO packet = redPacketServcie.get(getCommand);
        System.out.println(endDate - startDate);
        System.out.println(JSONObject.toJSONString(packet));


    }

}

