package com.damon.cqrs.sample.train.aggregate;


import com.damon.cqrs.domain.Aggregate;
import com.damon.cqrs.sample.train.aggregate.value_object.*;
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
 * 实现：购票、取消购票、站点区间购票数量保护（最多、最少可购票数量）
 *
 * @author xianpinglu
 */
public class TrainStock extends Aggregate {

    private final Integer AMPLIFICATION_FACTOR = 10000;
    /**
     * 站点到站点间的座位数量
     */
    private ConcurrentSkipListMap<Integer, BitSet> s2sSeatCountMap;
    /**
     * 用户购买的车票信息
     */
    private Map<Long, UserSeatInfo> userTicketMap;
    /**
     * 当前车次的总共座位数量
     */
    private Integer seatCount;

    private Map<Integer, Integer> stationSeatCountLimitMap;

    private Map<Integer, S2SMaxTicketCountProtectInfo> s2sSeatStrictProtectMapMap;

    private ConcurrentSkipListMap<Integer, S2SMaxTicketCountProtectInfo> s2sSeatRelaxedProtectMap;

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
     * 计算站点间卖了多少张票
     *
     * @param from
     * @param to
     * @param strict 是否结束站点区间严格匹配  不严格匹配：10005 可以匹配 10006  严格匹配： 10005 只能匹配10005
     * @return
     */
    private Integer calculateS2SSoldTicketCount(Integer from, Integer to, Boolean strict) {
        Long count = userTicketMap.values().stream().filter(info -> {
            if (strict) {
                return info.getStartStationNumber().equals(from) && info.getEndStationNumber().equals(to);
            } else {
                return info.getStartStationNumber().equals(from) && info.getEndStationNumber() <= to;
            }
        }).count();
        return count.intValue();
    }

    /**
     * 取消站点车票预留
     *
     * @param command
     */
    public S2S_TICKET_PROTECT_CANCEL_STATUS cancelProtectTicket(TicketProtectCancelCommand command) {
        if (command.getStrict()) {
            S2SMaxTicketCountProtectInfo protect = s2sSeatStrictProtectMapMap.get(key(command.getStartStationNumber(), command.getEndStationNumber()));
            if (protect != null) {
                TicketProtectCanceledEvent event = new TicketProtectCanceledEvent();
                event.setStartStationNumber(command.getStartStationNumber());
                event.setEndStationNumber(command.getEndStationNumber());
                event.setStrict(command.getStrict());
                super.applyNewEvent(event);
                return S2S_TICKET_PROTECT_CANCEL_STATUS.SUCCEED;
            }
        } else {
            S2SMaxTicketCountProtectInfo protect = s2sSeatRelaxedProtectMap.get(key(command.getStartStationNumber(), command.getEndStationNumber()));
            if (protect != null) {
                TicketProtectCanceledEvent event = new TicketProtectCanceledEvent();
                event.setStartStationNumber(command.getStartStationNumber());
                event.setEndStationNumber(command.getEndStationNumber());
                event.setStrict(command.getStrict());
                super.applyNewEvent(event);
                return S2S_TICKET_PROTECT_CANCEL_STATUS.SUCCEED;
            }
        }
        return S2S_TICKET_PROTECT_CANCEL_STATUS.NOT_EXIST;
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

    private Integer key(Integer from, Integer to) {
        return from * AMPLIFICATION_FACTOR + to;
    }

    private Integer fromKey(Integer from) {
        return from * AMPLIFICATION_FACTOR;
    }

    private Integer toKey(Integer to) {
        return (to - 1) * AMPLIFICATION_FACTOR + to;
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
        if (s2sSeatStrictProtectMapMap.get(key(command.getStartStationNumber(), command.getEndStationNumber())) != null) {
            return S2S_TICKET_PROTECT_STATUS.PROTECTED;
        }

        if (s2sSeatRelaxedProtectMap.get(key(command.getStartStationNumber(), command.getEndStationNumber())) != null) {
            return S2S_TICKET_PROTECT_STATUS.PROTECTED;
        }

        BitSet bitSet = new BitSet();
        for (BitSet set : s2sSeatCountMap.subMap(
                fromKey(command.getStartStationNumber()),
                Boolean.FALSE,
                toKey(command.getEndStationNumber()),
                Boolean.TRUE
        ).values()) {
            bitSet.or(set);
        }

        s2sSeatStrictProtectMapMap.values().forEach(strict -> {
            strict.getS2sProtectSeatIndexBitSet().subMap(
                    fromKey(command.getStartStationNumber()),
                    Boolean.FALSE,
                    toKey(command.getEndStationNumber()),
                    Boolean.TRUE
            ).values().forEach(set ->
                    bitSet.or(set)
            );
        });

        s2sSeatRelaxedProtectMap.values().forEach(protect -> {
            protect.getS2sProtectSeatIndexBitSet().subMap(
                    fromKey(command.getStartStationNumber()),
                    Boolean.FALSE,
                    toKey(command.getEndStationNumber()),
                    Boolean.TRUE
            ).values().forEach(set -> {
                bitSet.or(set);
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
        event.setProtectCanBuyTicketCount(command.getMinCanBuyTicketCount());
        event.setMaxCanBuyTicketCount(command.getMaxCanBuyTicketCount());
        event.setStartStationNumber(command.getStartStationNumber());
        event.setEndStationNumber(command.getEndStationNumber());
        event.setProtectSeatIndex(protectSeatIndexBitSet.toLongArray());
        event.setStrict(command.getStrict());
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

        if (userTicketMap.get(command.getUserId()) != null) {
            return new TicketBuyStatus(TICKET_BUY_STATUS.BOUGHT);
        }

        Integer stationLimitCount = stationSeatCountLimitMap.get(command.getStartStationNumber());
        if (stationLimitCount != null) {
            Long count = userTicketMap.values().stream().filter(info ->
                    info.getStartStationNumber().equals(command.getStartStationNumber())
            ).count();
            if (count.intValue() == stationLimitCount) {
                return new TicketBuyStatus(TICKET_BUY_STATUS.NOT_ENOUGH);
            }
        }

        //判断当前站点到目的站点是否有预留票，如果有预留票优先扣减预留票
        S2SMaxTicketCountProtectInfo s2sMaxSeatCountStrictProtect = s2sSeatStrictProtectMapMap.get(key(command.getStartStationNumber(), command.getEndStationNumber()));
        if (s2sMaxSeatCountStrictProtect != null) {
            int maxCanBuySeatCount = s2sMaxSeatCountStrictProtect.getMaxCanBuySeatCount();
            Integer s2sSoldTicketCount = calculateS2SSoldTicketCount(command.getStartStationNumber(), command.getEndStationNumber(), Boolean.TRUE);
            if (s2sSoldTicketCount >= maxCanBuySeatCount) {
                return new TicketBuyStatus(TICKET_BUY_STATUS.NOT_ENOUGH);
            }

            BitSet bs = new BitSet();
            bs.or(s2sMaxSeatCountStrictProtect.getSeatIndexBitSet());
            int protectCanBuySeatCount = s2sMaxSeatCountStrictProtect.getProtectCanBuySeatCount();
            s2sMaxSeatCountStrictProtect.getS2sProtectSeatIndexBitSet().subMap(
                    fromKey(command.getStartStationNumber()),
                    Boolean.FALSE,
                    toKey(command.getEndStationNumber()),
                    Boolean.TRUE
            ).values().forEach(set -> bs.andNot(set));
            if (bs.cardinality() < protectCanBuySeatCount) {
                BitSet bitSet = new BitSet();
                s2sMaxSeatCountStrictProtect.getS2sProtectSeatIndexBitSet().values().forEach(set ->
                        bitSet.or(set)
                );
                BitSet seatIndexBitSet = (BitSet) s2sMaxSeatCountStrictProtect.getSeatIndexBitSet().clone();
                seatIndexBitSet.andNot(bitSet);
                Integer seatIndex = bitSet.nextSetBit(0);
                if (seatIndex >= 0) {
                    TicketBoughtEvent event = new TicketBoughtEvent();
                    event.setUserId(command.getUserId());
                    event.setStartStationNumber(command.getStartStationNumber());
                    event.setEndStationNumber(command.getEndStationNumber());
                    event.setSeatIndex(seatIndex);
                    event.setSeatProtectType(SEAT_PROTECT_TYPE.STRICT_PROTECT);
                    super.applyNewEvent(event);
                    return new TicketBuyStatus(TICKET_BUY_STATUS.SUCCEED, seatIndex);
                }
            }
        }

        ConcurrentNavigableMap<Integer, S2SMaxTicketCountProtectInfo> map = s2sSeatRelaxedProtectMap.subMap(
                command.getStartStationNumber() * AMPLIFICATION_FACTOR + command.getEndStationNumber(),
                Boolean.TRUE,
                (command.getStartStationNumber() + 1) * AMPLIFICATION_FACTOR,
                Boolean.FALSE
        );

        for (Integer s2sSeatRelaxedProtectKey : map.keySet()) {
            S2SMaxTicketCountProtectInfo protect = map.get(s2sSeatRelaxedProtectKey);
            int maxCanBuySeatCount = protect.getMaxCanBuySeatCount();
            Integer s2sSoldTicketCount = calculateS2SSoldTicketCount(protect.getFromStation(), protect.getToStation(), Boolean.FALSE);
            if (s2sSoldTicketCount >= maxCanBuySeatCount) {
                return new TicketBuyStatus(TICKET_BUY_STATUS.NOT_ENOUGH);
            }

            BitSet bs = new BitSet();
            bs.or(protect.getSeatIndexBitSet());
            int protectCanBuySeatCount = protect.getProtectCanBuySeatCount();

            protect.getS2sProtectSeatIndexBitSet().subMap(
                    fromKey(command.getStartStationNumber()),
                    Boolean.FALSE,
                    toKey(command.getEndStationNumber()),
                    Boolean.TRUE
            ).values().forEach(s -> bs.andNot(s));
            if (bs.cardinality() < protectCanBuySeatCount) {
                BitSet bitSet = new BitSet();
                protect.getS2sProtectSeatIndexBitSet().values().forEach(set ->
                        bitSet.or(set)
                );
                BitSet seatIndexBitSet = (BitSet) protect.getSeatIndexBitSet().clone();
                seatIndexBitSet.andNot(bitSet);
                Integer seatIndex = bitSet.nextSetBit(0);
                if (seatIndex >= 0) {
                    TicketBoughtEvent event = new TicketBoughtEvent();
                    event.setUserId(command.getUserId());
                    event.setStartStationNumber(command.getStartStationNumber());
                    event.setEndStationNumber(command.getEndStationNumber());
                    event.setSeatIndex(seatIndex);
                    event.setS2sSeatRelaxedProtectKey(s2sSeatRelaxedProtectKey);
                    event.setSeatProtectType(SEAT_PROTECT_TYPE.RELAXED_PROTECT);
                    super.applyNewEvent(event);
                    return new TicketBuyStatus(TICKET_BUY_STATUS.SUCCEED, seatIndex);
                }
            }
        }

        BitSet bitSet = new BitSet();
        for (BitSet set : s2sSeatCountMap.subMap(
                fromKey(command.getStartStationNumber()),
                Boolean.FALSE,
                toKey(command.getEndStationNumber()),
                Boolean.TRUE
        ).values()) {
            bitSet.or(set);
        }

        s2sSeatStrictProtectMapMap.values().forEach(protect -> {
            protect.getS2sProtectSeatIndexBitSet().subMap(
                    fromKey(command.getStartStationNumber()),
                    Boolean.FALSE,
                    toKey(command.getEndStationNumber()),
                    Boolean.TRUE
            ).values().forEach(set -> {
                bitSet.or(set);
            });
        });

        s2sSeatRelaxedProtectMap.values().forEach(protect -> {
            protect.getS2sProtectSeatIndexBitSet().subMap(
                    fromKey(command.getStartStationNumber()),
                    Boolean.FALSE,
                    toKey(command.getEndStationNumber()),
                    Boolean.TRUE
            ).values().forEach(set -> {
                bitSet.or(set);
            });
        });


        //返回第一个设置为 false 的位的索引
        int seatIndex = bitSet.nextClearBit(0);
        //如果找不到座位索引或者座位的索引大于座位的总数-1，说明没有票了。
        if (seatIndex < 0 || seatIndex > seatCount - 1) {
            return new TicketBuyStatus(TICKET_BUY_STATUS.NOT_ENOUGH);
        }

        TicketBoughtEvent event = new TicketBoughtEvent();
        event.setUserId(command.getUserId());
        event.setStartStationNumber(command.getStartStationNumber());
        event.setEndStationNumber(command.getEndStationNumber());
        event.setSeatIndex(seatIndex);
        event.setSeatProtectType(SEAT_PROTECT_TYPE.GENERAL);
        super.applyNewEvent(event);
        return new TicketBuyStatus(TICKET_BUY_STATUS.SUCCEED, seatIndex);
    }

    /**
     * 取消购票
     *
     * @param command
     * @return
     */
    public TICKET_CANCEL_STATUS cancelTicket(TicketCancelCommand command) {
        UserSeatInfo info = userTicketMap.get(command.getUserId());
        if (info == null) {
            return TICKET_CANCEL_STATUS.NOT_EXIST;
        }

        TicketCanceledEvent event = new TicketCanceledEvent();
        event.setUserId(command.getUserId());
        event.setStartStationNumber(command.getStartStationNumber());
        event.setEndStationNumber(command.getEndStationNumber());
        event.setSeatIndex(info.getSeatIndex());
        event.setSeatProtectType(info.getType());
        super.applyNewEvent(event);
        return TICKET_CANCEL_STATUS.SUCCEED;
    }

    @SuppressWarnings("unused")
    private void apply(TicketBoughtEvent event) {

        Integer seatIndex = event.getSeatIndex();

        if (event.getSeatProtectType().equals(SEAT_PROTECT_TYPE.STRICT_PROTECT)) {
            S2SMaxTicketCountProtectInfo s2sMaxSeatCountProtect = s2sSeatStrictProtectMapMap.get(key(event.getStartStationNumber(), event.getEndStationNumber()));
            s2sMaxSeatCountProtect.getS2sProtectSeatIndexBitSet().values().forEach(set ->
                    set.set(seatIndex, Boolean.FALSE)
            );
        } else if (event.getSeatProtectType().equals(SEAT_PROTECT_TYPE.RELAXED_PROTECT)) {
            s2sSeatRelaxedProtectMap.get(event.getS2sSeatRelaxedProtectKey()).getS2sProtectSeatIndexBitSet().values().forEach(set ->
                    set.set(seatIndex, Boolean.FALSE)
            );
        }

        s2sSeatCountMap.subMap(
                fromKey(event.getStartStationNumber()),
                Boolean.FALSE,
                toKey(event.getEndStationNumber()),
                Boolean.TRUE
        ).forEach((number, set) -> {
            set.set(seatIndex);
        });

        UserSeatInfo trainSeatInfo = new UserSeatInfo();
        trainSeatInfo.setStartStationNumber(event.getStartStationNumber());
        trainSeatInfo.setEndStationNumber(event.getEndStationNumber());
        trainSeatInfo.setSeatIndex(event.getSeatIndex());
        trainSeatInfo.setType(event.getSeatProtectType());
        trainSeatInfo.setS2sSeatRelaxedProtectKey(event.getS2sSeatRelaxedProtectKey());
        userTicketMap.put(event.getUserId(), trainSeatInfo);
    }

    @SuppressWarnings("unused")
    private void apply(TrainCreatedEvent event) {
        this.userTicketMap = new HashMap<>();
        this.seatCount = event.getSeatCount();
        this.s2sSeatCountMap = new ConcurrentSkipListMap<>();
        this.stationSeatCountLimitMap = new HashMap<>();
        this.s2sSeatRelaxedProtectMap = new ConcurrentSkipListMap<>();
        this.s2sSeatStrictProtectMapMap = new HashMap<>();
        event.getStation2StationList().forEach(value ->
                this.s2sSeatCountMap.put(value, new BitSet(seatCount))
        );

    }

    @SuppressWarnings("unused")
    private void apply(TicketProtectCanceledEvent event) {
        if (event.getStrict()) {
            s2sSeatStrictProtectMapMap.remove(key(event.getStartStationNumber(), event.getEndStationNumber()));
        } else {
            s2sSeatRelaxedProtectMap.remove(key(event.getStartStationNumber(), event.getEndStationNumber()));
        }
    }

    @SuppressWarnings("unused")
    private void apply(TicketProtectSucceedEvent event) {

        ConcurrentSkipListMap<Integer, BitSet> s2sProtectSeatIndexBitSet = new ConcurrentSkipListMap<>();
        for (int start = event.getStartStationNumber(); start < event.getEndStationNumber(); start++) {
            s2sProtectSeatIndexBitSet.put(start * AMPLIFICATION_FACTOR + start + 1, BitSet.valueOf(event.getProtectSeatIndex()));
        }

        S2SMaxTicketCountProtectInfo protect = new S2SMaxTicketCountProtectInfo(
                event.getStartStationNumber(),
                event.getEndStationNumber(),
                event.getProtectCanBuyTicketCount(),
                event.getMaxCanBuyTicketCount(),
                BitSet.valueOf(event.getProtectSeatIndex()),
                event.getStrict(),
                s2sProtectSeatIndexBitSet
        );

        if (event.getStrict()) {
            s2sSeatStrictProtectMapMap.put(
                    key(event.getStartStationNumber(), event.getEndStationNumber()),
                    protect
            );
        } else {
            s2sSeatRelaxedProtectMap.put(
                    key(event.getStartStationNumber(), event.getEndStationNumber()),
                    protect
            );
        }
    }

    @SuppressWarnings("unused")
    private void apply(TicketCanceledEvent event) {

        UserSeatInfo info = userTicketMap.get(event.getUserId());
        if (info.getType().equals(SEAT_PROTECT_TYPE.STRICT_PROTECT)) {
            s2sSeatStrictProtectMapMap.get(
                    key(event.getStartStationNumber(), event.getEndStationNumber())
            ).getS2sProtectSeatIndexBitSet().subMap(
                    fromKey(event.getStartStationNumber()),
                    Boolean.FALSE,
                    toKey(event.getEndStationNumber()),
                    Boolean.TRUE
            ).values().forEach(set ->
                    set.set(info.getSeatIndex(), Boolean.FALSE)
            );

        } else if (info.getType().equals(SEAT_PROTECT_TYPE.RELAXED_PROTECT)) {
            s2sSeatRelaxedProtectMap.get(info.getS2sSeatRelaxedProtectKey()).getS2sProtectSeatIndexBitSet().subMap(
                    fromKey(event.getStartStationNumber()),
                    Boolean.FALSE,
                    toKey(event.getEndStationNumber()),
                    Boolean.TRUE
            ).values().forEach(set ->
                    set.set(info.getSeatIndex(), Boolean.FALSE)
            );
        }
        s2sSeatCountMap.subMap(
                fromKey(event.getStartStationNumber()),
                Boolean.FALSE,
                toKey(event.getEndStationNumber()),
                Boolean.TRUE
        ).values().forEach(set ->
                set.set(info.getSeatIndex(), Boolean.FALSE)
        );
        userTicketMap.remove(event.getUserId());
    }

    @SuppressWarnings("unused")
    private void apply(StationTicketLimitEvent event) {
        stationSeatCountLimitMap.put(event.getStationNumber(), event.getMaxCanBuyTicketCount());
    }

    @Override
    public long createSnapshootCycle() {
        return -1;
    }

    public ConcurrentSkipListMap<Integer, BitSet> getS2sSeatCountMap() {
        return s2sSeatCountMap;
    }

    public Map<Long, UserSeatInfo> getUserTicketMap() {
        return userTicketMap;
    }

    public Integer getSeatCount() {
        return seatCount;
    }


}

