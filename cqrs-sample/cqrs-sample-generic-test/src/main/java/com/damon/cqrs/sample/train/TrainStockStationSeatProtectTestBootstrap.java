package com.damon.cqrs.sample.train;

import com.alibaba.fastjson.JSONObject;
import com.damon.cqrs.event.EventCommittingService;
import com.damon.cqrs.sample.red_packet.domain_service.CqrsConfig;
import com.damon.cqrs.sample.train.command.*;
import com.damon.cqrs.sample.train.domain.TrainStockDoaminService;
import com.damon.cqrs.sample.train.dto.TrainStockDTO;
import com.damon.cqrs.utils.IdWorker;
import org.apache.rocketmq.client.exception.MQClientException;

import java.util.ArrayList;
import java.util.List;

/**
 * 站点间车票数量限制测试
 *
 *
 */
public class TrainStockStationSeatProtectTestBootstrap {
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
        TicketProtectCommand protectCommand4 = new TicketProtectCommand(IdWorker.getId(), id);
        protectCommand4.setStartStationNumber(1);
        protectCommand4.setEndStationNumber(6);
        protectCommand4.setMinCanBuyTicketCount(50);
        protectCommand4.setMaxCanBuyTicketCount(50);
        System.out.println("----------预留车票  1:6-50  -------------");
        System.out.println(service.protectTicket(protectCommand4));


        TicketProtectCommand protectCommand = new TicketProtectCommand(IdWorker.getId(), id);
        protectCommand.setStartStationNumber(1);
        protectCommand.setEndStationNumber(6);
        protectCommand.setMinCanBuyTicketCount(50);
        protectCommand.setMaxCanBuyTicketCount(50);
        System.out.println("----------预留车票  1:6-50  -------------");
        System.out.println(service.protectTicket(protectCommand));


        TicketProtectCommand protectCommand3 = new TicketProtectCommand(IdWorker.getId(), id);
        protectCommand3.setStartStationNumber(1);
        protectCommand3.setEndStationNumber(5);
        protectCommand3.setMinCanBuyTicketCount(50);
        protectCommand3.setMaxCanBuyTicketCount(50);
        System.out.println("----------预留车票  1:5-50  -------------");
        System.out.println(service.protectTicket(protectCommand3));

        TicketProtectCommand protectCommand2 = new TicketProtectCommand(IdWorker.getId(), id);
        protectCommand2.setStartStationNumber(5);
        protectCommand2.setEndStationNumber(6);
        protectCommand2.setMinCanBuyTicketCount(11);
        protectCommand2.setMaxCanBuyTicketCount(11);
        System.out.println("----------预留车票  5:6-11  -------------");
        System.out.println(service.protectTicket(protectCommand2));
        Thread.sleep(1000);
        getTrainStackInfo(service, id);


        TicketProtectCommand protectCommand5 = new TicketProtectCommand(IdWorker.getId(), id);
        protectCommand5.setStartStationNumber(1);
        protectCommand5.setEndStationNumber(3);
        protectCommand5.setMinCanBuyTicketCount(11);
        protectCommand5.setMaxCanBuyTicketCount(11);
        System.out.println("----------预留车票  1:3-11  -------------");
        System.out.println(service.protectTicket(protectCommand5));
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

