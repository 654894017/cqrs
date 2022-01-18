package com.damon.cqrs.sample.train.domain;


import com.damon.cqrs.domain.Aggregate;
import com.damon.cqrs.sample.train.command.TicketBuyCommand;
import com.damon.cqrs.sample.train.command.TicketCancelCommand;
import com.damon.cqrs.sample.train.event.TicketBoughtEvent;
import com.damon.cqrs.sample.train.event.TicketCanceledEvent;
import com.damon.cqrs.sample.train.event.TrainCreatedEvent;

import java.util.BitSet;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.ConcurrentNavigableMap;
import java.util.concurrent.ConcurrentSkipListMap;

public class TrainStock extends Aggregate {

    private ConcurrentSkipListMap<Integer, StationSeatInfo> s2sSeatCount;

    private Map<Long, TrainSeatInfo> userTicket;

    public TrainStock() {

    }

    public TrainStock(Long id, Map<Integer, Integer> s2sSeatCount) {
        super(id);
        if (s2sSeatCount.isEmpty()) {
            throw new IllegalArgumentException("station interval seat count not allowed to be empty");
        }
        TrainCreatedEvent event = new TrainCreatedEvent();
        ConcurrentSkipListMap<Integer, StationSeatInfo> s2ssc = new ConcurrentSkipListMap<>();
        s2sSeatCount.forEach((key, value) -> {
            s2ssc.put(key, new StationSeatInfo(new BitSet(value), value));
        });
        event.setS2sSeatCount(s2ssc);
        super.applyNewEvent(event);
    }

    /**
     * 购票
     *
     * @param command
     * @return
     */
    public TicketBuyStatus buyTicket(TicketBuyCommand command) {

        if (userTicket.get(command.getUserId()) != null) {
            return new TicketBuyStatus(TICKET_BUY_STAUTS.BOUGHT);
        }
        ConcurrentNavigableMap<Integer, StationSeatInfo> map = s2sSeatCount.subMap(
                command.getStartStationNumber() * 10000,
                (command.getEndStationNumber() - 1) * 10000 + command.getEndStationNumber() + 1
        );

        for (StationSeatInfo info : map.values()) {
            //bitset被true的个数等于站点到站点的可购买票数，那么车票不足，不可购买。反之可以购买
            if (info.getBigSet().cardinality() == info.getCount()) {
                return new TicketBuyStatus(TICKET_BUY_STAUTS.NOT_ENOUGH);
            }
        }

        //获取没有被占用的第一个座位索引
        int index = map.values().stream().findFirst().get().getBigSet().nextClearBit(0);

        TicketBoughtEvent event = new TicketBoughtEvent();
        event.setUserId(command.getUserId());
        event.setStartStationNumber(command.getStartStationNumber());
        event.setEndStationNumber(command.getEndStationNumber());
        event.setSeatIndex(index);
        super.applyNewEvent(event);
        return new TicketBuyStatus(TICKET_BUY_STAUTS.SUCCEED, index);
    }

    /**
     * 取消购票
     *
     * @param command
     * @return
     */
    public TICKET_CANCEL_STAUTS cancelTicket(TicketCancelCommand command) {
        TrainSeatInfo info = userTicket.get(command.getUserId());
        if (info == null) {
            return TICKET_CANCEL_STAUTS.NOT_EXIST;
        }

        TicketCanceledEvent event = new TicketCanceledEvent();
        event.setUserId(command.getUserId());
        event.setStartStationNumber(command.getStartStationNumber());
        event.setEndStationNumber(command.getEndStationNumber());
        event.setSeatIndex(info.getSeatIndex());
        super.applyNewEvent(event);
        return TICKET_CANCEL_STAUTS.SUCCEED;
    }

    @SuppressWarnings("unused")
    private void apply(TicketBoughtEvent event) {
        ConcurrentNavigableMap<Integer, StationSeatInfo> map = s2sSeatCount.subMap(
                event.getStartStationNumber() * 10000,
                (event.getEndStationNumber() - 1) * 10000 + event.getEndStationNumber() + 1
        );

        map.forEach((num, info) -> {
            info.getBigSet().set(event.getSeatIndex(), Boolean.TRUE);
        });

        TrainSeatInfo trainSeatInfo = new TrainSeatInfo();
        trainSeatInfo.setStartStation(event.getStartStationNumber());
        trainSeatInfo.setEndStation(event.getEndStationNumber());
        trainSeatInfo.setSeatIndex(event.getSeatIndex());
        userTicket.put(event.getUserId(), trainSeatInfo);
    }

    @SuppressWarnings("unused")
    private void apply(TrainCreatedEvent event) {
        this.userTicket = new HashMap<>();
        this.s2sSeatCount = event.getS2sSeatCount();
    }

    @SuppressWarnings("unused")
    private void apply(TicketCanceledEvent event) {
        ConcurrentNavigableMap<Integer, StationSeatInfo> map = s2sSeatCount.subMap(
                event.getStartStationNumber() * 10000,
                (event.getEndStationNumber() - 1) * 10000 + event.getEndStationNumber() + 1
        );
        map.forEach((num, info) -> {
            info.getBigSet().set(event.getSeatIndex(), Boolean.FALSE);
        });
        userTicket.remove(event.getUserId());
    }

    @Override
    public long createSnapshootCycle() {
        return -1;
    }

    public ConcurrentSkipListMap<Integer, StationSeatInfo> getS2sSeatCount() {
        return s2sSeatCount;
    }

    public Map<Long, TrainSeatInfo> getUserTicket() {
        return userTicket;
    }

    public enum TICKET_BUY_STAUTS {SUCCEED, NOT_ENOUGH, BOUGHT}

    public enum TICKET_CANCEL_STAUTS {SUCCEED, NOT_EXIST}

    public static class TicketBuyStatus {

        private final TICKET_BUY_STAUTS stauts;

        private Integer seatIndex;

        public TicketBuyStatus(TICKET_BUY_STAUTS stauts, Integer seatIndex) {
            this.stauts = stauts;
            this.seatIndex = seatIndex;
        }

        public TicketBuyStatus(TICKET_BUY_STAUTS stauts) {
            this.stauts = stauts;
        }

        public Integer getSeatIndex() {
            return seatIndex;
        }

        public void setSeatIndex(Integer seatIndex) {
            this.seatIndex = seatIndex;
        }

        public TICKET_BUY_STAUTS getStauts() {
            return stauts;
        }
    }

    public static class StationSeatInfo {
        private BitSet bigSet;
        private Integer count;

        public StationSeatInfo() {

        }

        public StationSeatInfo(BitSet bigSet, Integer count) {
            this.bigSet = bigSet;
            this.count = count;
        }

        public BitSet getBigSet() {
            return bigSet;
        }

        public void setBigSet(BitSet bigSet) {
            this.bigSet = bigSet;
        }

        public Integer getCount() {
            return count;
        }

        public void setCount(Integer count) {
            this.count = count;
        }
    }

    /**
     * 车次坐席信息
     */
    public static class TrainSeatInfo {

        private Integer startStation;
        private Integer endStation;
        private Integer seatIndex;

        public Integer getStartStation() {
            return startStation;
        }

        public void setStartStation(Integer startStation) {
            this.startStation = startStation;
        }

        public Integer getEndStation() {
            return endStation;
        }

        public void setEndStation(Integer endStation) {
            this.endStation = endStation;
        }

        public Integer getSeatIndex() {
            return seatIndex;
        }

        public void setSeatIndex(Integer seatIndex) {
            this.seatIndex = seatIndex;
        }
    }
}

