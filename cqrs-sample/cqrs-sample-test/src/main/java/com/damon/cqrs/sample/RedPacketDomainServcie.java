package com.damon.cqrs.sample;

import com.damon.cqrs.*;
import com.damon.cqrs.event_store.MysqlEventOffset;
import com.damon.cqrs.event_store.MysqlEventStore;
import com.damon.cqrs.mq.DefaultMQProducer;
import com.damon.cqrs.mq.RocketMQSendSyncService;
import com.zaxxer.hikari.HikariDataSource;
import org.apache.rocketmq.client.exception.MQClientException;

import java.util.Date;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CountDownLatch;

/**
 * @author xianpinglu
 */
public class RedPacketDomainServcie extends AbstractDomainService<WeixinRedPacket> implements IRedPacketDomainServcie {

    public RedPacketDomainServcie(EventCommittingService eventCommittingService) {
        super(eventCommittingService);
    }

    @Override
    public void createRedPackage(RedPacketCreateCommand command) {
        super.process(command, () ->
                new WeixinRedPacket(command.getAggregateId(), command.getMoney(), command.getNumber(), command.getType(), command.getSponsorId())
        ).join();
        return;
    }

    @Override
    public int grabRedPackage(RedPacketGrabCommand command) {
        CompletableFuture<Integer> future = super.process(command, redPacket -> redPacket.grabRedPackage(command.getUserId()));
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

    public static HikariDataSource dataSource() {
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
        //RocketMQSendSyncService rocketmqService = new RocketMQSendSyncService(producer, "TTTTTT", 5);
        //EventSendingService sendingService = new EventSendingService(rocketmqService, 50, 1024);
        //new DefaultEventSendingShceduler(store, offset, sendingService, 5, 5);
        return new EventCommittingService(store, aggregateSnapshootService, aggregateCache, 1024, 2048);

    }
    public static void main(String[] args) throws InterruptedException, MQClientException {
        EventCommittingService committingService = init();
        RedPacketDomainServcie redPacketServcie = new RedPacketDomainServcie(committingService);
        Long id = IdWorker.getId();
        RedPacketCreateCommand create = new RedPacketCreateCommand(IdWorker.getId(),id);
        create.setMoney(1000000L);
        create.setNumber(1000000);
        create.setSponsorId(1L);
        create.setType(RedPacketTypeEnum.AVG);
        redPacketServcie.createRedPackage(create);

        Long startDate = System.currentTimeMillis();
        CountDownLatch latch = new CountDownLatch(1000000);
        for(int i=0;i<500;i++){
            new Thread(()->{
                for(int number = 0; number < 2000; number++) {
                    RedPacketGrabCommand grabCommand = new RedPacketGrabCommand(IdWorker.getId(), id);
                    grabCommand.setUserId(IdWorker.getId());
                    redPacketServcie.grabRedPackage(grabCommand);
                    latch.countDown();
                }
            }).start();
        }
        latch.await();
        Long endDate = System.currentTimeMillis();
        System.out.println(endDate-startDate);


    }

}
