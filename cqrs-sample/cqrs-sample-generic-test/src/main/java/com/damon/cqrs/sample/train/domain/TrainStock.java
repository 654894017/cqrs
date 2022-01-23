package com.damon.cqrs.sample.train.domain;


import com.damon.cqrs.domain.Aggregate;
import com.damon.cqrs.sample.train.command.*;
import com.damon.cqrs.sample.train.event.*;

import java.util.BitSet;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentNavigableMap;
import java.util.concurrent.ConcurrentSkipListMap;

/**
 * 车次聚合根
 * <p>
 * <p>
 * 实现：购票、取消购票、站点区间购票数量保护（最多、最少可购票数量）、站点最多购票数量限制
 *
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
    private Map<String, ConcurrentSkipListMap<Integer, BitSet>> s2sSeatProtectMap;
    /**
     * 站点到站点间的预留车票。 key: 1:6  value: 1111  4个1表示  从0开始保留坐标为0，1，2，3个座位
     */
    private Map<String, BitSet> s2sSeatProtectIndexMap;
    /**
     * 站点到站点间最大售票数量  key: 1:6  value: 100  表示站点1到站点6  最多能够购买100张票。
     */
    private Map<String, Integer> s2sMaxTicketCountProtectMap;
    /**
     * 站点限制购买票数量限制
     */
    private Map<Integer, Integer> stationSeatCountLimitMap;

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
        if (s2sSeatProtectIndexMap.get(command.getStartStationNumber() + ":" + command.getEndStationNumber()) != null) {
            TicketProtectCanceledEvent event = new TicketProtectCanceledEvent();
            event.setStartStationNumber(command.getStartStationNumber());
            event.setEndStationNumber(command.getEndStationNumber());
            super.applyNewEvent(event);
            return TICKET_PROTECT_CANCEL_STATUS.SUCCEED;
        }
        return TICKET_PROTECT_CANCEL_STATUS.NOT_EXIST;
    }

    /**
     * 限制某个站点上车最大人数
     *
     * @param command
     * @return
     */
    public STATION_TICKET_LIMIT_STATUS limitStationTicket(StationTicketLimitCommand command) {
        super.applyNewEvent(new StationTicketLimitEvent(command.getStationNumber(), command.getMaxCanBuyTicketCount()));
        return STATION_TICKET_LIMIT_STATUS.SUCCEED;
    }

    /**
     * 预留站点票
     *
     * @param command
     * @return
     */
    public S2S_TICKET_PROTECT_STATUS protectS2STicket(TicketProtectCommand command) {

        if (command.getMaxCanBuyTicketCount() < 0) {
            throw new IllegalArgumentException("The maximum number of tickets that can be purchased between stations cannot be less than 0");
        }
        if (command.getMinCanBuyTicketCount() < 0) {
            throw new IllegalArgumentException("The number of reserved tickets between stations should not be less than 0");
        }

        //区间已被预留不允许重复预留
        if (s2sSeatProtectMap.get(command.getStartStationNumber() + ":" + command.getEndStationNumber()) != null) {
            return S2S_TICKET_PROTECT_STATUS.PROTECTED;
        }

        BitSet bitSet = new BitSet();
        for (BitSet set : s2sSeatCount.subMap(
                command.getStartStationNumber() * 10000,
                (command.getEndStationNumber() - 1) * 10000 + command.getEndStationNumber() + 1).values()) {
            bitSet.or(set);
        }

        s2sSeatProtectMap.values().forEach(map -> {
            map.subMap(command.getStartStationNumber() * 10000, (command.getEndStationNumber() - 1) * 10000 + command.getEndStationNumber() + 1).values().forEach(set -> {
                // TRUE XOR TRUE = FALSE , FALSE XOR FALSE = FALSE , TRUE OXR FALSE = TRUE
                bitSet.xor(set);
            });
        });

        //bitset or运算后，如果索引对应的值是true的说明已经被预留了。
        int count = 0, tempIndex = 0;
        BitSet protectSeatIndexBitSet = new BitSet();
        for (; ; ) {
            //判断当前空余的票是否满足预留要求。返回第一个为false的索引位置
            int index = bitSet.nextClearBit(tempIndex);
            if (index < 0 || (index > seatCount - 1)) {
                return S2S_TICKET_PROTECT_STATUS.NOT_ENOUGH;
            }
            protectSeatIndexBitSet.set(index);
            ++count;
            tempIndex = index + 1;
            //剩余的票满足预留要求，跳出循环
            if (count == command.getMinCanBuyTicketCount()) {
                break;
            }
        }

        TicketProtectSucceedEvent event = new TicketProtectSucceedEvent();
        event.setCount(command.getMinCanBuyTicketCount());
        event.setMaxCanBuyTicketCount(command.getMaxCanBuyTicketCount());
        event.setStartStationNumber(command.getStartStationNumber());
        event.setEndStationNumber(command.getEndStationNumber());
        event.setProtectSeatIndex(protectSeatIndexBitSet.toLongArray());
        super.applyNewEvent(event);
        return S2S_TICKET_PROTECT_STATUS.SUCCEED;
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

        Integer stationLimitCount = stationSeatCountLimitMap.get(command.getStartStationNumber());
        if (stationLimitCount != null) {
            Long count = userTicket.values().stream().filter(info ->
                    info.getStartStationNumber().equals(command.getStartStationNumber())
            ).count();
            if (count.intValue() == stationLimitCount) {
                return new TicketBuyStatus(TICKET_BUY_STAUTS.NOT_ENOUGH);
            }
        }

        //判断当前站点到目的站点是否有预留票，如果有预留票优先扣减预留票
        BitSet s2sSeatProtectIndexBitSet = s2sSeatProtectIndexMap.get(command.getStartStationNumber() + ":" + command.getEndStationNumber());
        if (s2sSeatProtectIndexBitSet != null) {
            //计算站点到站点间累计卖票数量（包含超卖的数量）
            Long userS2SBoughtTicketCount = userTicket.values().stream().filter(info ->
                    info.getStartStationNumber().equals(command.getStartStationNumber())
                            && info.getEndStationNumber().equals(command.getEndStationNumber()
                    )
            ).count();

            //检查是否超过预留票的最大限制数量。例如：站点1-6预留60个座位，最多限制购买80个座位,如果已购买的票大于最大区间购买票数，提示无票
            if (userS2SBoughtTicketCount.intValue() >= s2sMaxTicketCountProtectMap.get(command.getStartStationNumber() + ":" + command.getEndStationNumber())) {
                return new TicketBuyStatus(TICKET_BUY_STAUTS.NOT_ENOUGH);
            }

            BitSet bitSet = (BitSet) s2sSeatProtectIndexBitSet.clone();
            s2sSeatProtectMap.values().forEach(map -> {
                map.subMap(command.getStartStationNumber() * 10000,
                        (command.getEndStationNumber() - 1) * 10000 + command.getEndStationNumber() + 1).values().forEach(set -> {
                    bitSet.and(set);
                });
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
                (command.getEndStationNumber() - 1) * 10000 + command.getEndStationNumber() + 1).values()
        ) {
            bitSet.or(set);
        }

        s2sSeatProtectMap.values().forEach(map -> {
            map.subMap(command.getStartStationNumber() * 10000, (command.getEndStationNumber() - 1) * 10000 + command.getEndStationNumber() + 1).values().forEach(set -> {
                bitSet.or(set);
            });
        });

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


        s2sSeatProtectMap.values().forEach(map -> {
            map.subMap(event.getStartStationNumber() * 10000, (event.getEndStationNumber() - 1) * 10000 + event.getEndStationNumber() + 1).values().forEach(set -> {
                set.set(event.getSeatIndex(), Boolean.FALSE);
            });
        });

        TrainSeatInfo trainSeatInfo = new TrainSeatInfo();
        trainSeatInfo.setStartStationNumber(event.getStartStationNumber());
        trainSeatInfo.setEndStationNumber(event.getEndStationNumber());
        trainSeatInfo.setSeatIndex(event.getSeatIndex());
        userTicket.put(event.getUserId(), trainSeatInfo);
    }

    @SuppressWarnings("unused")
    private void apply(TrainCreatedEvent event) {
        this.userTicket = new HashMap<>();
        this.seatCount = event.getSeatCount();
        this.s2sSeatProtectMap = new ConcurrentSkipListMap<>();
        this.s2sSeatCount = new ConcurrentSkipListMap<>();
        this.s2sSeatProtectIndexMap = new HashMap<>();
        this.s2sMaxTicketCountProtectMap = new HashMap<>();
        this.stationSeatCountLimitMap = new HashMap<>();
        event.getStation2StationList().forEach(value ->
                s2sSeatCount.put(value, new BitSet(seatCount))
        );

    }

    @SuppressWarnings("unused")
    private void apply(TicketProtectCanceledEvent event) {
        s2sSeatProtectMap.remove(event.getStartStationNumber() + ":" + event.getEndStationNumber());
        s2sSeatProtectIndexMap.remove(event.getStartStationNumber() + ":" + event.getEndStationNumber());
        s2sMaxTicketCountProtectMap.remove(event.getStartStationNumber() + ":" + event.getEndStationNumber());
    }

    @SuppressWarnings("unused")
    private void apply(TicketProtectSucceedEvent event) {
        BitSet bitSet = new BitSet();
        for (BitSet set : s2sSeatCount.values()) {
            bitSet.or(set);
        }
        ConcurrentSkipListMap<Integer, BitSet> protectMap = s2sSeatProtectMap.get(event.getStartStationNumber() + ":" + event.getEndStationNumber());
        if (protectMap != null) {
            for (BitSet set : protectMap.values()) {
                bitSet.or(set);
            }
        }

        BitSet protectBitset = new BitSet();
        for (int i = 0; i < event.getCount(); i++) {
            Integer temp = bitSet.nextClearBit(0);
            bitSet.set(temp, Boolean.TRUE);
            protectBitset.set(temp);
        }
        ConcurrentSkipListMap<Integer, BitSet> map = new ConcurrentSkipListMap<>();
        for (int start = event.getStartStationNumber(); start < event.getEndStationNumber(); start++) {
            map.put(start * 10000 + start + 1, BitSet.valueOf(event.getProtectSeatIndex()));
        }
        s2sSeatProtectMap.put(event.getStartStationNumber() + ":" + event.getEndStationNumber(), map);

        s2sSeatProtectIndexMap.put(event.getStartStationNumber() + ":" + event.getEndStationNumber(), BitSet.valueOf(event.getProtectSeatIndex()));

        s2sMaxTicketCountProtectMap.put(event.getStartStationNumber() + ":" + event.getEndStationNumber(), event.getMaxCanBuyTicketCount());

    }


    @SuppressWarnings("unused")
    private void apply(TicketCanceledEvent event) {

        BitSet temp = s2sSeatProtectIndexMap.get(event.getStartStationNumber() + ":" + event.getEndStationNumber());
        //如果的是普通票（不是站点预留票），直接退回到票池里。
        if (temp != null) {
            //如果退的票是站点预留票，那么当前的票需要返回站点预留票保护池。
            s2sSeatProtectMap.get(event.getStartStationNumber() + ":" + event.getEndStationNumber()).subMap(
                    event.getStartStationNumber() * 10000,
                    (event.getEndStationNumber() - 1) * 10000 + event.getEndStationNumber() + 1
            ).values().forEach(set ->
                    set.set(event.getSeatIndex(), Boolean.TRUE)
            );
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

    @SuppressWarnings("unused")
    private void apply(StationTicketLimitEvent event) {
        stationSeatCountLimitMap.put(event.getStationNumber(), event.getMaxCanBuyTicketCount());
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

    public enum S2S_TICKET_PROTECT_STATUS {SUCCEED, PROTECTED, NOT_ENOUGH}

    public enum STATION_TICKET_LIMIT_STATUS {SUCCEED, FAILED}

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
     * 站点到站点座位的限制
     */
    public static class S2SSeatProtect {
        private BitSet seatIndexBitSet;
        private Integer s2sMaxTicketProtectCount;

        public S2SSeatProtect(BitSet seatIndexBitSet, Integer s2sMaxTicketProtectCount) {
            this.seatIndexBitSet = seatIndexBitSet;
            this.s2sMaxTicketProtectCount = s2sMaxTicketProtectCount;
        }

        public BitSet getSeatIndexBitSet() {
            return seatIndexBitSet;
        }

        public void setSeatIndexBitSet(BitSet seatIndexBitSet) {
            this.seatIndexBitSet = seatIndexBitSet;
        }

        public Integer getS2sMaxTicketProtectCount() {
            return s2sMaxTicketProtectCount;
        }

        public void setS2sMaxTicketProtectCount(Integer s2sMaxTicketProtectCount) {
            this.s2sMaxTicketProtectCount = s2sMaxTicketProtectCount;
        }
    }

    /**
     * 用户车次坐席信息
     */
    public static class TrainSeatInfo {

        private Integer startStationNumber;

        private Integer endStationNumber;

        private Integer seatIndex;

        public Integer getStartStationNumber() {
            return startStationNumber;
        }

        public void setStartStationNumber(Integer startStationNumber) {
            this.startStationNumber = startStationNumber;
        }

        public Integer getEndStationNumber() {
            return endStationNumber;
        }

        public void setEndStationNumber(Integer endStationNumber) {
            this.endStationNumber = endStationNumber;
        }

        public Integer getSeatIndex() {
            return seatIndex;
        }

        public void setSeatIndex(Integer seatIndex) {
            this.seatIndex = seatIndex;
        }
    }
}

