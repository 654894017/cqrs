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

/**
 * @author xianpinglu
 */
public class TrainStock extends Aggregate {
    /**
     * 站点到站点间的座位数量
     */
    private ConcurrentSkipListMap<Integer, BitSet> s2sSeatCount;
    /**
     * 用户购买的车票信息
     */
    private Map<Long, TrainSeatInfo> userTicket;
    /**
     * 当前车次的总共座位数量
     */
    private Integer seatCount;
    /**
     * 站点到站点间的预留车票。 key: 10002  value: 100  表示1站点到6站点预留100个座位。
     */
    private ConcurrentSkipListMap<Integer, BitSet> s2sSeatProtectMap;
    /**
     * 站点到站点间的预留车票。 key: 1:6  value: 1111  4个1表示  从0开始保留坐标为0，1，2，3个座位
     */
    private Map<String, BitSet> s2sSeatProtectFlagMap;

    public TrainStock() {

    }

    public TrainStock(Long id, List<Integer> station2StationList, int seatCount) {
        super(id);
        if (seatCount < 1) {
            throw new IllegalArgumentException("seat count min 1.");
        }
        if (station2StationList.isEmpty()) {
            throw new IllegalArgumentException("station interval seat count not allowed to be empty");
        }

        TrainCreatedEvent event = new TrainCreatedEvent();
        event.setSeatCount(seatCount);
        event.setSation2StaionList(station2StationList);
        super.applyNewEvent(event);
    }

    /**
     * 取消站点车票预留
     *
     * @param command
     */
    public TICKET_PROTECT_CANCEL_STATUS cancelProtectTicket(TicketProtectCancelCommand command) {
        if (s2sSeatProtectFlagMap.get(command.getStartStationNumber() + ":" + command.getEndStationNumber()) != null) {
            TicketProtectCanceledEvent event = new TicketProtectCanceledEvent();
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
        for (BitSet set : s2sSeatCount.subMap(
                command.getStartStationNumber() * 10000,
                (command.getEndStationNumber() - 1) * 10000 + command.getEndStationNumber() + 1).values()) {
            bitSet.or(set);
        }

        for (BitSet set : s2sSeatProtectMap.subMap(
                command.getStartStationNumber() * 10000,
                (command.getEndStationNumber() - 1) * 10000 + command.getEndStationNumber() + 1).values()) {
            bitSet.or(set);
        }

        //bitset or运算后，如果索引对应的值是true的说明已经被预留了。
        int count = 0, tempIndex = 0;
        BitSet protectSeatIndexBitSet = new BitSet();
        for (; ; ) {
            //判断当前空余的票是否满足预留要求。
            int index = bitSet.nextClearBit(tempIndex);
            if (index < 0 || (index > seatCount - 1)) {
                return TICKET_PROTECT_STATUS.NOT_ENOUGH;
            }
            protectSeatIndexBitSet.set(index);
            ++count;
            tempIndex = index + 1;
            //剩余的票满足预留要求，跳出循环
            if (count == command.getCount()) {
                break;
            }
        }

        TicketProtectSucceedEvent event = new TicketProtectSucceedEvent();
        event.setCount(command.getCount());
        event.setStartStationNumber(command.getStartStationNumber());
        event.setEndStationNumber(command.getEndStationNumber());
        event.setProtectSeatIndex(protectSeatIndexBitSet.toLongArray());
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
        //判断当前站点到目的站点是否有预留票，如果有预留票优先扣减预留票
        BitSet temp = s2sSeatProtectFlagMap.get(command.getStartStationNumber() + ":" + command.getEndStationNumber());
        if (temp != null) {
            ConcurrentNavigableMap<Integer, BitSet> info = s2sSeatProtectMap.subMap(command.getStartStationNumber() * 10000,
                    (command.getEndStationNumber() - 1) * 10000 + command.getEndStationNumber() + 1);
            BitSet bitSet = new BitSet();
            info.forEach((number, set) -> {
                bitSet.or(set);
            });

            int seatIndex = bitSet.nextSetBit(0);
            if (seatIndex >= 0) {
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
        for (BitSet set : s2sSeatCount.subMap(
                command.getStartStationNumber() * 10000,
                (command.getEndStationNumber() - 1) * 10000 + command.getEndStationNumber() + 1).values()) {
            bitSet.or(set);
        }

        for (BitSet set : s2sSeatProtectMap.subMap(command.getStartStationNumber() * 10000,
                (command.getEndStationNumber() - 1) * 10000 + command.getEndStationNumber() + 1).values()) {
            bitSet.or(set);
        }


        int seatIndex = bitSet.nextClearBit(0);
        //如果找不到座位索引或者座位的索引大于座位的总数-1，说明没有票了。
        if (seatIndex < 0 || seatIndex > seatCount - 1) {
            return new TicketBuyStatus(TICKET_BUY_STAUTS.NOT_ENOUGH);
        }

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

        s2sSeatCount.subMap(
                event.getStartStationNumber() * 10000,
                (event.getEndStationNumber() - 1) * 10000 + event.getEndStationNumber() + 1
        ).forEach((number, set) -> {
            set.set(event.getSeatIndex());
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
        this.seatCount = event.getSeatCount();
        this.s2sSeatProtectMap = new ConcurrentSkipListMap<>();
        this.s2sSeatCount = new ConcurrentSkipListMap<>();
        this.s2sSeatProtectFlagMap = new HashMap<>();
        event.getStation2StationList().forEach(value ->
                s2sSeatCount.put(value, new BitSet(seatCount))
        );

    }

    @SuppressWarnings("unused")
    private void apply(TicketProtectCanceledEvent event) {
        for (int start = event.getStartStationNumber(); start < event.getEndStationNumber(); start++) {
            s2sSeatProtectMap.remove(start * 10000 + start + 1);
        }
        s2sSeatProtectFlagMap.remove(event.getStartStationNumber() + ":" + event.getEndStationNumber());
    }

    @SuppressWarnings("unused")
    private void apply(TicketProtectSucceedEvent event) {
        BitSet bitSet = new BitSet();
        for (BitSet set : s2sSeatCount.values()) {
            bitSet.or(set);
        }

        for (BitSet set : s2sSeatProtectMap.values()) {
            bitSet.or(set);
        }

        BitSet protectBitset = new BitSet();
        for (int i = 0; i < event.getCount(); i++) {
            Integer temp = bitSet.nextClearBit(0);
            bitSet.set(temp, Boolean.TRUE);
            protectBitset.set(temp);
        }

        for (int start = event.getStartStationNumber(); start < event.getEndStationNumber(); start++) {
            s2sSeatProtectMap.put(start * 10000 + start + 1, BitSet.valueOf(event.getProtectSeatIndex()));
        }

        s2sSeatProtectFlagMap.put(event.getStartStationNumber() + ":" + event.getEndStationNumber(), BitSet.valueOf(event.getProtectSeatIndex()));

    }


    @SuppressWarnings("unused")
    private void apply(TicketCanceledEvent event) {

        BitSet temp = s2sSeatProtectFlagMap.get(event.getStartStationNumber() + ":" + event.getEndStationNumber());
        //如果的是普通票（不是站点预留票），直接退回到票池里。
        if (temp != null) {
            //如果退的票是站点预留票，那么当前的票需要返回站点预留票保护池。
            ConcurrentNavigableMap<Integer, BitSet> map = s2sSeatProtectMap.subMap(
                    event.getStartStationNumber() * 10000,
                    (event.getEndStationNumber() - 1) * 10000 + event.getEndStationNumber() + 1
            );
            map.forEach((num, set) -> {
                set.set(event.getSeatIndex(), Boolean.TRUE);
            });
        }
        ConcurrentNavigableMap<Integer, BitSet> map = s2sSeatCount.subMap(
                event.getStartStationNumber() * 10000,
                (event.getEndStationNumber() - 1) * 10000 + event.getEndStationNumber() + 1
        );
        map.forEach((num, set) -> {
            set.set(event.getSeatIndex(), Boolean.FALSE);
        });

        userTicket.remove(event.getUserId());
    }

    @Override
    public long createSnapshootCycle() {
        return -1;
    }

    public ConcurrentSkipListMap<Integer, BitSet> getS2sSeatCount() {
        return s2sSeatCount;
    }

    public Map<Long, TrainSeatInfo> getUserTicket() {
        return userTicket;
    }

    public Integer getSeatCount() {
        return seatCount;
    }

    public enum TICKET_BUY_STAUTS {SUCCEED, NOT_ENOUGH, BOUGHT}

    public enum TICKET_PROTECT_CANCEL_STATUS {SUCCEED, NOT_EXIST}

    public enum TICKET_PROTECT_STATUS {SUCCEED, NOT_ENOUGH}

    public enum TICKET_CANCEL_STAUTS {SUCCEED, NOT_EXIST}

    public static class StationSeatProtectInfo {

        private BitSet protectSeatBitSet;
        private BitSet protectSeatIndexBitSet;

        public StationSeatProtectInfo(BitSet protectSeatBitSet, BitSet protectSeatIndexBitSet) {
            this.protectSeatBitSet = protectSeatBitSet;
            this.protectSeatIndexBitSet = protectSeatIndexBitSet;
        }

        public BitSet getProtectSeatBitSet() {
            return protectSeatBitSet;
        }

        public void setProtectSeatBitSet(BitSet protectSeatBitSet) {
            this.protectSeatBitSet = protectSeatBitSet;
        }

        public BitSet getProtectSeatIndexBitSet() {
            return protectSeatIndexBitSet;
        }

        public void setProtectSeatIndexBitSet(BitSet protectSeatIndexBitSet) {
            this.protectSeatIndexBitSet = protectSeatIndexBitSet;
        }
    }

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


    /**
     * 用户车次坐席信息
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

