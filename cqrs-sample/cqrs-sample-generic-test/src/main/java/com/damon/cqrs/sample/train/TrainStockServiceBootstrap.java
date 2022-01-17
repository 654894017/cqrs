package com.damon.cqrs.sample.train;

import com.damon.cqrs.event.EventCommittingService;
import com.damon.cqrs.sample.red_packet.domain_service.CqrsConfig;
import com.damon.cqrs.sample.train.command.TicketBuyCommand;
import com.damon.cqrs.sample.train.command.TicketCancelCommand;
import com.damon.cqrs.sample.train.command.TrainCreateCommand;
import com.damon.cqrs.sample.train.domain.TrainStockDoaminService;
import com.damon.cqrs.utils.IdWorker;
import org.apache.rocketmq.client.exception.MQClientException;

import java.util.concurrent.ConcurrentSkipListMap;
import java.util.concurrent.LinkedBlockingQueue;

public class TrainStockServiceBootstrap {
    public static void main(String[] args) throws MQClientException, InterruptedException {
        EventCommittingService committingService = CqrsConfig.init();
        TrainStockDoaminService service = new TrainStockDoaminService(committingService);

        // 假设有6个站点分别为1，2，3，4，5
        // 10002，100 表示站点1到站点2的票为100
        // 20003，100 表示站点2到站点3的票为100
        // 如果用户需要购买站点1到站点3的票，首先我们需要判断1到2（10002），2到3（20003）站点的余票是否大于0，如果大于0说明可以购票，然后分别扣减10002，10003分别减1。
        ConcurrentSkipListMap<Integer, Integer> map = new ConcurrentSkipListMap<>();
        map.put(10002, 100);
        map.put(20003, 100);
        map.put(30004, 100);
        map.put(40005, 100);
        map.put(50006, 100);
        Long id = 202201170001L;
        TrainCreateCommand create = new TrainCreateCommand(IdWorker.getId(), id);
        create.setS2sSeatCount(map);
        service.createTrain(create);

        LinkedBlockingQueue<Long> userIds = new LinkedBlockingQueue<>();

        //购买票
        for (int i = 0; i < 100; i++) {
            TicketBuyCommand command = new TicketBuyCommand(IdWorker.getId(), id);
            command.setStartStationNumber(1);
            command.setEndStationNumber(6);
            Long userId = IdWorker.getId();
            command.setUserId(userId);
            userIds.add(userId);
            System.out.println(service.buyTicket(command));
        }
        System.out.println(map);

        //取消购票
        for (int i = 0; i < 100; i++) {
            TicketCancelCommand command = new TicketCancelCommand(IdWorker.getId(), id);
            command.setStartStationNumber(1);
            command.setEndStationNumber(6);
            command.setUserId(userIds.take());
            System.out.println(service.cancelTicket(command));
        }
        System.out.println(map);
    }
}

