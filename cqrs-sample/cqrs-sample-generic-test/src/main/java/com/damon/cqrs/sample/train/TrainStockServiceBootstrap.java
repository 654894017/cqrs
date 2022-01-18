package com.damon.cqrs.sample.train;

import com.alibaba.fastjson.JSONObject;
import com.damon.cqrs.event.EventCommittingService;
import com.damon.cqrs.sample.red_packet.domain_service.CqrsConfig;
import com.damon.cqrs.sample.train.command.TicketBuyCommand;
import com.damon.cqrs.sample.train.command.TicketCancelCommand;
import com.damon.cqrs.sample.train.command.TicketGetCommand;
import com.damon.cqrs.sample.train.command.TrainCreateCommand;
import com.damon.cqrs.sample.train.domain.TrainStock;
import com.damon.cqrs.sample.train.domain.TrainStockDoaminService;
import com.damon.cqrs.sample.train.dto.TrainStockDTO;
import com.damon.cqrs.utils.IdWorker;
import org.apache.rocketmq.client.exception.MQClientException;

import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.ConcurrentSkipListMap;
import java.util.concurrent.LinkedBlockingQueue;

public class TrainStockServiceBootstrap {
    public static void main(String[] args) throws MQClientException, InterruptedException {
        EventCommittingService committingService = CqrsConfig.init();
        TrainStockDoaminService service = new TrainStockDoaminService(committingService);

        // 假设某个车次有6个站点分别为1，2，3，4，5，6。共计100个1等座位
        // 10002，100 表示站点1到站点2的票为100
        // 20003，100 表示站点2到站点3的票为100
        // 如果用户需要购买站点1到站点3的票，首先我们需要判断1到2（10002），2到3（20003）站点的余票是否大于0，如果大于0说明可以购票，然后分别扣减10002，10003分别减1。
        Map<Integer, Integer> map = new HashMap<>();
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
        for (int i = 0; i < 102; i++) {
            TicketBuyCommand command = new TicketBuyCommand(IdWorker.getId(), id);
            command.setStartStationNumber(1);
            command.setEndStationNumber(6);
            Long userId = IdWorker.getId();
            command.setUserId(userId);
            userIds.add(userId);

            TrainStock.TicketBuyStatus status = service.buyTicket(command);
            if (status.getStauts().equals(TrainStock.TICKET_BUY_STAUTS.SUCCEED)) {
                System.out.println("购买成功，座位号：" + status.getSeatIndex());
            } else {
                System.err.println("购买失败，失败信息：" + status.getStauts());
            }
        }
        getTrainStackInfo(service, id);

        //取消购票
        for (int i = 0; i < 100; i++) {
            TicketCancelCommand command = new TicketCancelCommand(IdWorker.getId(), id);
            command.setStartStationNumber(1);
            command.setEndStationNumber(6);
            command.setUserId(userIds.take());
            System.out.println(service.cancelTicket(command));
        }
        getTrainStackInfo(service, id);

    }

    public static void getTrainStackInfo(TrainStockDoaminService service, Long id) {
        //获取车次信息（包含座位信息）
        TicketGetCommand command = new TicketGetCommand(IdWorker.getId(), id);

        TrainStockDTO trainStock = service.getTrain(command);

        System.out.println(JSONObject.toJSONString(trainStock));
    }

}

