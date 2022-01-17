package com.damon.cqrs.sample.train.domain;


import com.damon.cqrs.domain.Aggregate;
import com.damon.cqrs.sample.train.command.TicketBuyCommand;
import com.damon.cqrs.sample.train.command.TicketCancelCommand;
import com.damon.cqrs.sample.train.event.TicketBoughtEvent;
import com.damon.cqrs.sample.train.event.TicketCanceledEvent;
import com.damon.cqrs.sample.train.event.TrainCreatedEvent;

import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.ConcurrentNavigableMap;
import java.util.concurrent.ConcurrentSkipListMap;

public class TrainStock extends Aggregate {

    private ConcurrentSkipListMap<Integer, Integer> s2sSeatCount;

    private Map<Long, Boolean> userTicket;

    public TrainStock() {

    }

    public TrainStock(Long id, ConcurrentSkipListMap<Integer, Integer> s2sSeatCount) {
        super(id);
        TrainCreatedEvent event = new TrainCreatedEvent();
        event.setS2sSeatCount(s2sSeatCount);
        super.applyNewEvent(event);
    }

    /**
     * 购票
     *
     * @param command
     * @return
     */
    public int buyTicket(TicketBuyCommand command) {
        if (userTicket.get(command.getUserId()) != null) {
            return -2;
        }
        ConcurrentNavigableMap<Integer, Integer> map = s2sSeatCount.subMap(
                command.getStartStationNumber() * 10000,
                (command.getEndStationNumber() - 1) * 10000 + command.getEndStationNumber() + 1
        );
        for (int count : map.values()) {
            //判断是否区间是否有票,如果等于0说明区间无票
            if (count == 0) {
                return -1;
            }
        }
        TicketBoughtEvent event = new TicketBoughtEvent();
        event.setUserId(command.getUserId());
        event.setStartStationNumber(command.getStartStationNumber());
        event.setEndStationNumber(command.getEndStationNumber());
        super.applyNewEvent(event);
        return 1;
    }

    /**
     * 取消购票
     *
     * @param command
     * @return
     */
    public int cancelTicket(TicketCancelCommand command) {
        if (!userTicket.get(command.getUserId())) {
            return -1;
        }

        TicketCanceledEvent event = new TicketCanceledEvent();
        event.setUserId(command.getUserId());
        event.setStartStationNumber(command.getStartStationNumber());
        event.setEndStationNumber(command.getEndStationNumber());
        super.applyNewEvent(event);
        return 1;
    }

    @SuppressWarnings("unused")
    private void apply(TicketBoughtEvent event) {
        ConcurrentNavigableMap<Integer, Integer> map = s2sSeatCount.subMap(
                event.getStartStationNumber() * 10000,
                (event.getEndStationNumber() - 1) * 10000 + event.getEndStationNumber() + 1
        );
        map.forEach((num, count) -> {
            map.put(num, --count);
        });
        userTicket.put(event.getUserId(), true);
    }

    @SuppressWarnings("unused")
    private void apply(TrainCreatedEvent event) {
        this.userTicket = new HashMap<>();
        this.s2sSeatCount = event.getS2sSeatCount();
    }

    @SuppressWarnings("unused")
    private void apply(TicketCanceledEvent event) {
        ConcurrentNavigableMap<Integer, Integer> map = s2sSeatCount.subMap(
                event.getStartStationNumber() * 10000,
                (event.getEndStationNumber() - 1) * 10000 + event.getEndStationNumber() + 1
        );
        map.forEach((num, count) -> {
            map.put(num, ++count);
        });
    }

    @Override
    public long createSnapshootCycle() {
        return -1;
    }
}

