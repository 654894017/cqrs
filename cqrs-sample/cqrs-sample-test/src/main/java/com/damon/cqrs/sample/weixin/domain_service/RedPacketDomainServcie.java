package com.damon.cqrs.sample.weixin.domain_service;

import com.alibaba.fastjson.JSONObject;
import com.damon.cqrs.*;
import com.damon.cqrs.event.EventCommittingService;
import com.damon.cqrs.event.EventSendingService;
import com.damon.cqrs.event_store.MysqlEventOffset;
import com.damon.cqrs.event_store.MysqlEventStore;
import com.damon.cqrs.rocketmq.RocketMQSendSyncService;
import com.damon.cqrs.rocketmq.core.DefaultMQProducer;
import com.damon.cqrs.sample.weixin.aggregate.WeixinRedPacket;
import com.damon.cqrs.sample.weixin.command.RedPacketCreateCommand;
import com.damon.cqrs.sample.weixin.command.RedPacketGetCommand;
import com.damon.cqrs.sample.weixin.command.RedPacketGrabCommand;
import com.damon.cqrs.sample.weixin.command.RedPacketTypeEnum;
import com.damon.cqrs.sample.weixin.dto.WeixinRedPacketDTO;
import com.damon.cqrs.store.IEventOffset;
import com.damon.cqrs.store.IEventStore;
import com.damon.cqrs.utils.IdWorker;
import com.zaxxer.hikari.HikariDataSource;
import org.apache.rocketmq.client.exception.MQClientException;

import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CountDownLatch;

/**
 * @author xianpinglu
 */
public class RedPacketDomainServcie extends AbstractDomainService<WeixinRedPacket> implements IRedPacketDomainServcie {

    public RedPacketDomainServcie(EventCommittingService eventCommittingService) {
        super(eventCommittingService);
    }

    private static HikariDataSource dataSource() {
        HikariDataSource dataSource = new HikariDataSource();
        dataSource.setJdbcUrl("jdbc:mysql://localhost:3306/cqrs?serverTimezone=UTC&rewriteBatchedStatements=true");
        dataSource.setUsername("root");
        dataSource.setPassword("root");
        dataSource.setMaximumPoolSize(5);
        dataSource.setMinimumIdle(5);
        dataSource.setDriverClassName(com.mysql.cj.jdbc.Driver.class.getTypeName());
        return dataSource;
    }

    public static EventCommittingService init() throws MQClientException {
        IEventStore store = new MysqlEventStore(dataSource());
        IEventOffset offset = new MysqlEventOffset(dataSource());
        IAggregateSnapshootService aggregateSnapshootService = new DefaultAggregateSnapshootService(50, 5);
        IAggregateCache aggregateCache = new DefaultAggregateGuavaCache(1024 * 1024, 30);
        DefaultMQProducer producer = new DefaultMQProducer();
        producer.setNamesrvAddr("localhost:9876");
        producer.setProducerGroup("test");
        //producer.start();
        RocketMQSendSyncService rocketmqService = new RocketMQSendSyncService(producer, "cqrs_event_queue", 5);
        EventSendingService sendingService = new EventSendingService(rocketmqService, 50, 1024);
        // new DefaultEventSendingShceduler(store, offset, sendingService, 5, 5);
        return new EventCommittingService(store, aggregateSnapshootService, aggregateCache, 128, 2048);

    }

    public static void main(String[] args) throws InterruptedException, MQClientException {
        EventCommittingService committingService = init();
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

    @Override
    public void createRedPackage(RedPacketCreateCommand command) {
        super.process(command, () ->
                new WeixinRedPacket(
                        command.getAggregateId(),
                        command.getMoney(),
                        command.getNumber(),
                        command.getType(),
                        command.getSponsorId()
                )
        ).join();
        return;
    }

    @Override
    public int grabRedPackage(final RedPacketGrabCommand command) {
        CompletableFuture<Integer> future = super.process(
                command,
                redPacket -> redPacket.grabRedPackage(command.getUserId())
        );

        return future.join();
    }

    @Override
    public WeixinRedPacketDTO get(RedPacketGetCommand command) {
        CompletableFuture<WeixinRedPacketDTO> future = super.process(
                command,
                redPacket -> {
                    WeixinRedPacketDTO redPacketDTO = new WeixinRedPacketDTO();
                    redPacketDTO.setMap(redPacket.getMap());
                    redPacketDTO.setId(redPacket.getId());
                    redPacketDTO.setRedpacketStack(redPacket.getRedpacketStack());
                    redPacketDTO.setSponsorId(redPacket.getSponsorId());
                    redPacketDTO.setType(redPacket.getType());
                    return redPacketDTO;
                }
        );
        return future.join();
    }

    @Override
    public CompletableFuture<WeixinRedPacket> getAggregateSnapshoot(long aggregateId, Class<WeixinRedPacket> classes) {
        return CompletableFuture.completedFuture(null);
    }

    @Override
    public CompletableFuture<Boolean> saveAggregateSnapshoot(WeixinRedPacket aggregate) {
        return CompletableFuture.completedFuture(true);
    }

}
