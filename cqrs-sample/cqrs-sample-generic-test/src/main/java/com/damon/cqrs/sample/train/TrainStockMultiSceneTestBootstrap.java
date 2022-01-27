package com.damon.cqrs.sample.train;

import com.alibaba.fastjson.JSONObject;
import com.damon.cqrs.event.EventCommittingService;
import com.damon.cqrs.sample.red_packet.domain_service.CqrsConfig;
import com.damon.cqrs.sample.train.command.*;
import com.damon.cqrs.sample.train.aggregate.value_object.TICKET_BUY_STATUS;
import com.damon.cqrs.sample.train.aggregate.value_object.TicketBuyStatus;
import com.damon.cqrs.sample.train.damain_service.TrainStockDoaminService;
import com.damon.cqrs.sample.train.dto.TrainStockDTO;
import com.damon.cqrs.utils.IdWorker;
import org.apache.rocketmq.client.exception.MQClientException;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.LinkedBlockingQueue;

/**
 * 多场景测试
 */
public class TrainStockMultiSceneTestBootstrap {
    public static void main(String[] args) throws MQClientException, InterruptedException {
        EventCommittingService committingService = CqrsConfig.init();
        TrainStockDoaminService service = new TrainStockDoaminService(committingService);

        // 假设某个车次有6个站点分别为1，2，3，4，5，6。共计100个1等座位
        // 10002，100 表示站点1到站点2的票为100
        // 20003，100 表示站点2到站点3的票为100
        // 如果用户需要购买站点1到站点3的票，首先我们需要判断1到2（10002），2到3（20003）站点的余票是否大于0，如果大于0说明可以购票，然后分别扣减10002，10003分别减1。
        List<Integer> list = new ArrayList<>();
        list.add(10002);
        list.add(20003);
        list.add(30004);
        list.add(40005);
        list.add(50006);
        Long id = 202201170001L;
        TrainCreateCommand create = new TrainCreateCommand(IdWorker.getId(), id);
        create.setS2s(list);
        create.setSeatCount(100);
        service.createTrain(create);
        TicketProtectCommand protectCommand = new TicketProtectCommand(IdWorker.getId(), id);
        protectCommand.setStartStationNumber(1);
        protectCommand.setEndStationNumber(6);
        protectCommand.setMinCanBuyTicketCount(50);
        protectCommand.setMaxCanBuyTicketCount(50);
        protectCommand.setStrict(Boolean.TRUE);
        System.out.println("----------预留车票  1:6-50  -------------");
        System.out.println(service.protectTicket(protectCommand));
        LinkedBlockingQueue<Long> userIds = new LinkedBlockingQueue<>();
        System.out.println("----------开始购票 1-5 -------------");
        //购买票
        for (int i = 0; i < 52; i++) {
            TicketBuyCommand command = new TicketBuyCommand(IdWorker.getId(), id);
            command.setStartStationNumber(1);
            command.setEndStationNumber(5);
            Long userId = IdWorker.getId();
            command.setUserId(userId);
            TicketBuyStatus status = service.buyTicket(command);
            if (status.getStauts().equals(TICKET_BUY_STATUS.SUCCEED)) {
                userIds.add(userId);
                System.out.println("购买成功，座位号：" + status.getSeatIndex());
            } else {
                System.err.println("购买失败，失败信息：" + status.getStauts());
            }
        }
        Thread.sleep(1000);
        getTrainStackInfo(service, id);
        TicketProtectCancelCommand cancelCommandCommand = new TicketProtectCancelCommand(IdWorker.getId(), id);
        cancelCommandCommand.setStartStationNumber(1);
        cancelCommandCommand.setEndStationNumber(6);
        cancelCommandCommand.setStrict(Boolean.TRUE);
        System.out.println("----------取消保护-------------");
        System.out.println(service.cancelProtectTicket(cancelCommandCommand));

        System.out.println("----------开始购票2 1-6 -------------");
        //购买票
        for (int i = 0; i < 50; i++) {
            TicketBuyCommand command = new TicketBuyCommand(IdWorker.getId(), id);
            command.setStartStationNumber(1);
            command.setEndStationNumber(6);
            Long userId = IdWorker.getId();
            command.setUserId(userId);
            TicketBuyStatus status = service.buyTicket(command);
            if (status.getStauts().equals(TICKET_BUY_STATUS.SUCCEED)) {
                userIds.add(userId);
                System.out.println("购买成功，座位号：" + status.getSeatIndex());
            } else {
                System.err.println("购买失败，失败信息：" + status.getStauts());
            }
        }
        Thread.sleep(1000);
        getTrainStackInfo(service, id);

        System.out.println("----------取消购票-------------");

        //取消购票
        for (int i = 0; i < 10; i++) {
            TicketCancelCommand command = new TicketCancelCommand(IdWorker.getId(), id);
            command.setStartStationNumber(1);
            command.setEndStationNumber(6);
            command.setUserId(userIds.take());
            System.out.println(service.cancelTicket(command));
        }
        Thread.sleep(1000);
        getTrainStackInfo(service, id);
        System.out.println("----------开始购票3 1-6 -------------");
        //购买票
        for (int i = 0; i < 11; i++) {
            TicketBuyCommand command = new TicketBuyCommand(IdWorker.getId(), id);
            command.setStartStationNumber(1);
            command.setEndStationNumber(5);
            Long userId = IdWorker.getId();
            command.setUserId(userId);
            TicketBuyStatus status = service.buyTicket(command);
            if (status.getStauts().equals(TICKET_BUY_STATUS.SUCCEED)) {
                userIds.add(userId);
                System.out.println("购买成功，座位号：" + status.getSeatIndex());
            } else {
                System.err.println("购买失败，失败信息：" + status.getStauts());
            }
        }
        Thread.sleep(1000);
        getTrainStackInfo(service, id);


//        System.out.println("----------开始购票4 5-6 -------------");
//        //购买票
//        for (int i = 0; i < 30; i++) {
//            TicketBuyCommand command = new TicketBuyCommand(IdWorker.getId(), id);
//            command.setStartStationNumber(5);
//            command.setEndStationNumber(6);
//            Long userId = IdWorker.getId();
//            command.setUserId(userId);
//            TrainStock.TicketBuyStatus status = service.buyTicket(command);
//            if (status.getStauts().equals(TrainStock.TICKET_BUY_STAUTS.SUCCEED)) {
//                userIds.add(userId);
//                System.out.println("购买成功，座位号：" + status.getSeatIndex());
//            } else {
//                System.err.println("购买失败，失败信息：" + status.getStauts());
//            }
//        }
//        Thread.sleep(1000);
//        getTrainStackInfo(service, id);

        TicketProtectCommand protectCommand2 = new TicketProtectCommand(IdWorker.getId(), id);
        protectCommand2.setStartStationNumber(5);
        protectCommand2.setEndStationNumber(6);
        protectCommand2.setMinCanBuyTicketCount(1);
        protectCommand2.setMaxCanBuyTicketCount(2);
        protectCommand2.setStrict(Boolean.TRUE);
        System.out.println("----------预留车票  5:6-5  -------------");
        System.out.println(service.protectTicket(protectCommand2));
        Thread.sleep(1000);
        getTrainStackInfo(service, id);

        //购买票
        for (int i = 0; i < 3; i++) {
            TicketBuyCommand command = new TicketBuyCommand(IdWorker.getId(), id);
            command.setStartStationNumber(5);
            command.setEndStationNumber(6);
            Long userId = IdWorker.getId();
            command.setUserId(userId);
            TicketBuyStatus status = service.buyTicket(command);
            if (status.getStauts().equals(TICKET_BUY_STATUS.SUCCEED)) {
                userIds.add(userId);
                System.out.println("购买成功，座位号：" + status.getSeatIndex());
            } else {
                System.err.println("购买失败，失败信息：" + status.getStauts());
            }
        }
        Thread.sleep(1000);
        getTrainStackInfo(service, id);


    }

    public static void getTrainStackInfo(TrainStockDoaminService service, Long id) {
        //获取车次信息（包含座位信息）
        TicketGetCommand command = new TicketGetCommand(IdWorker.getId(), id);

        TrainStockDTO trainStock = service.getTrain(command);

        System.out.println(JSONObject.toJSONString(trainStock));
    }

}

