package com.damon.cqrs.sample.train.domain;


import com.damon.cqrs.domain.Aggregate;
import com.damon.cqrs.sample.train.command.TicketBuyCommand;
import com.damon.cqrs.sample.train.command.TicketCancelCommand;
import com.damon.cqrs.sample.train.command.TicketProtectCancelCommand;
import com.damon.cqrs.sample.train.command.TicketProtectCommand;
import com.damon.cqrs.sample.train.event.*;

import java.util.BitSet;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentNavigableMap;
import java.util.concurrent.ConcurrentSkipListMap;

public class TrainStock extends Aggregate {

    private ConcurrentSkipListMap<Integer, StationSeatInfo> s2sSeatCount;

    private Map<Long, TrainSeatInfo> userTicket;

    private Integer seatCount;

    private Map<String, SeatProtectInfo> s2sSeatProtectMap;

    public TrainStock() {

    }

    public TrainStock(Long id, List<Integer> s2s, Integer seatCount) {
        super(id);
        if (s2s.isEmpty()) {
            throw new IllegalArgumentException("station interval seat count not allowed to be empty");
        }
        TrainCreatedEvent event = new TrainCreatedEvent();
        ConcurrentSkipListMap<Integer, StationSeatInfo> s2ssc = new ConcurrentSkipListMap<>();
        s2s.forEach(value -> {
            s2ssc.put(value, new StationSeatInfo(new BitSet(seatCount), seatCount, 0));
        });
        this.seatCount = seatCount;
        event.setS2sSeatCount(s2ssc);
        this.s2sSeatProtectMap = new HashMap<>();
        super.applyNewEvent(event);
    }

    /**
     * 取消站点车票预留
     *
     * @param command
     */
    public TICKET_PROTECT_CANCEL_STATUS cancelProtectTicket(TicketProtectCancelCommand command) {
        if (s2sSeatProtectMap.get(command.getStartStationNumber() + ":" + command.getEndStationNumber()) != null) {
            TicketProtectCancelEvent event = new TicketProtectCancelEvent();
            event.setStartStationNumber(command.getStartStationNumber());
            event.setEndStationNumber(command.getEndStationNumber());
            super.applyNewEvent(event);
            return TICKET_PROTECT_CANCEL_STATUS.SUCCEED;
        }
        return TICKET_PROTECT_CANCEL_STATUS.NOT_EXIST;
    }

    /**
     * 预留站点票
     *
     * @param command
     * @return
     */
    public TICKET_PROTECT_STATUS protectTicket(TicketProtectCommand command) {

        BitSet bitSet = new BitSet();
        for (StationSeatInfo info : s2sSeatCount.values()) {
            bitSet.or(info.getBigSet());
        }

        for (SeatProtectInfo info : s2sSeatProtectMap.values()) {
            bitSet.or(info.getSeatProtectBitSet());
        }
        int count = 0, tempIndex = 0;
        for (; ; ) {
            int index = bitSet.nextClearBit(tempIndex);
            if (index < 0 || (index > seatCount - 1)) {
                return TICKET_PROTECT_STATUS.NOT_ENOUGH;
            }
            ++count;
            tempIndex = index + 1;
            if (count == command.getCount()) {
                break;
            }
        }

        TicketProtectSucceedEvent event = new TicketProtectSucceedEvent();
        event.setCount(command.getCount());
        event.setStartStationNumber(command.getStartStationNumber());
        event.setEndStationNumber(command.getEndStationNumber());
        super.applyNewEvent(event);
        return TICKET_PROTECT_STATUS.SUCCEED;
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

        SeatProtectInfo seatProtectInfo = s2sSeatProtectMap.get(command.getStartStationNumber() + ":" + command.getEndStationNumber());
        if (seatProtectInfo != null) {
            BitSet bitSet = seatProtectInfo.getSeatProtectBitSet();
            if (bitSet.cardinality() > 0) {
                int seatIndex = bitSet.nextSetBit(0);
                bitSet.set(seatIndex, Boolean.FALSE);
                ConcurrentNavigableMap<Integer, StationSeatInfo> map = s2sSeatCount.subMap(
                        command.getStartStationNumber() * 10000,
                        (command.getEndStationNumber() - 1) * 10000 + command.getEndStationNumber() + 1);
                map.forEach((number, info) -> {
                    info.getBigSet().set(seatIndex, Boolean.TRUE);
                });

                TicketBoughtEvent event = new TicketBoughtEvent();
                event.setUserId(command.getUserId());
                event.setStartStationNumber(command.getStartStationNumber());
                event.setEndStationNumber(command.getEndStationNumber());
                event.setSeatIndex(seatIndex);
                super.applyNewEvent(event);

                return new TicketBuyStatus(TICKET_BUY_STAUTS.SUCCEED, seatIndex);
            }

        }

        BitSet bitSet = new BitSet();
        for (StationSeatInfo info : s2sSeatCount.values()) {
            bitSet.or(info.getBigSet());
        }

        for (SeatProtectInfo info : s2sSeatProtectMap.values()) {
            bitSet.or(info.getSeatProtectBitSet());
        }


        int seatIndex = bitSet.nextClearBit(0);
        //如果找不到座位索引或者座位的索引大于座位的总数-1，说明没有票了。
        if (seatIndex < 0 || seatIndex > seatCount - 1) {
            return new TicketBuyStatus(TICKET_BUY_STAUTS.NOT_ENOUGH);
        }

        s2sSeatCount.subMap(
                command.getStartStationNumber() * 10000,
                command.getEndStationNumber() * 10000 + command.getEndStationNumber()
        ).forEach((number, info) -> {
            info.getBigSet().set(seatIndex);
        });

        TicketBoughtEvent event = new TicketBoughtEvent();
        event.setUserId(command.getUserId());
        event.setStartStationNumber(command.getStartStationNumber());
        event.setEndStationNumber(command.getEndStationNumber());
        event.setSeatIndex(seatIndex);
        super.applyNewEvent(event);
        return new TicketBuyStatus(TICKET_BUY_STAUTS.SUCCEED, seatIndex);
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
    private void apply(TicketProtectCancelEvent event) {
        s2sSeatProtectMap.remove(event.getStartStationNumber()+":" + event.getEndStationNumber());
    }

    @SuppressWarnings("unused")
    private void apply(TicketProtectSucceedEvent event) {
        BitSet bitSet = new BitSet();
        for (StationSeatInfo info : s2sSeatCount.values()) {
            bitSet.or(info.getBigSet());
        }

        for (SeatProtectInfo info : s2sSeatProtectMap.values()) {
            bitSet.or(info.getSeatProtectBitSet());
        }

        BitSet protectBitset = new BitSet();
        for (int i = 0; i < event.getCount(); i++) {
            Integer temp = bitSet.nextClearBit(0);
            bitSet.set(temp, Boolean.TRUE);
            protectBitset.set(temp);
        }

        s2sSeatProtectMap.put(event.getStartStationNumber() + ":" + event.getEndStationNumber(), new SeatProtectInfo(protectBitset));
    }


    @SuppressWarnings("unused")
    private void apply(TicketCanceledEvent event) {

        SeatProtectInfo protectInfo = s2sSeatProtectMap.get(event.getStartStationNumber() + ":" + event.getEndStationNumber());

        if (protectInfo == null) {
            ConcurrentNavigableMap<Integer, StationSeatInfo> map = s2sSeatCount.subMap(
                    event.getStartStationNumber() * 10000,
                    (event.getEndStationNumber() - 1) * 10000 + event.getEndStationNumber() + 1
            );
            map.forEach((num, info) -> {
                info.getBigSet().set(event.getSeatIndex(), Boolean.FALSE);
            });
        } else {
            boolean bool = protectInfo.getOriginalSeatProtectBitSet().get(event.getSeatIndex());
            if (bool) {
                protectInfo.getSeatProtectBitSet().set(event.getSeatIndex(), bool);
                s2sSeatCount.get(event.getSeatIndex()).getBigSet().set(event.getSeatIndex(), Boolean.FALSE);
            }

        }

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

    public enum TICKET_PROTECT_CANCEL_STATUS {SUCCEED, NOT_EXIST}

    public enum TICKET_PROTECT_STATUS {SUCCEED, NOT_ENOUGH}

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

    public static class SeatProtectInfo {
        private BitSet originalSeatProtectBitSet;
        private BitSet seatProtectBitSet;

        public SeatProtectInfo(BitSet seatProtectBitSet) {
            this.seatProtectBitSet = seatProtectBitSet;
            this.originalSeatProtectBitSet = seatProtectBitSet;
        }

        public BitSet getSeatProtectBitSet() {
            return seatProtectBitSet;
        }

        public void setSeatProtectBitSet(BitSet seatProtectBitSet) {
            this.seatProtectBitSet = seatProtectBitSet;
        }

        public BitSet getOriginalSeatProtectBitSet() {
            return originalSeatProtectBitSet;
        }

        public void setOriginalSeatProtectBitSet(BitSet originalSeatProtectBitSet) {
            this.originalSeatProtectBitSet = originalSeatProtectBitSet;
        }
    }

    public static class StationSeatInfo {

        private Integer protectCount;
        private BitSet bigSet;
        private Integer count;

        public StationSeatInfo() {

        }

        public StationSeatInfo(BitSet bigSet, Integer count, Integer protectCount) {
            this.bigSet = bigSet;
            this.count = count;
            this.protectCount = protectCount;
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

        public Integer getProtectCount() {
            return protectCount;
        }

        public void setProtectCount(Integer protectCount) {
            this.protectCount = protectCount;
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

