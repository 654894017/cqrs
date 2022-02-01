package com.damon.cqrs.sample.red_packet.aggregate;

import com.damon.cqrs.domain.Aggregate;
import com.damon.cqrs.sample.red_packet.command.RedPacketTypeEnum;
import com.damon.cqrs.sample.red_packet.event.RedPacketCreatedEvent;
import com.damon.cqrs.sample.red_packet.event.RedPacketGrabSucceedEvent;

import java.util.HashMap;
import java.util.Map;
import java.util.Stack;

/**
 * 微信红包聚合根
 *
 * @author xianpinglu
 */
public class WeixinRedPacket extends Aggregate {
    /**
     *
     */
    private static final long serialVersionUID = 1L;

    /**
     * key 用户id   value  抢到的金额
     */
    private Map<Long, Long> map;

    private Stack<Long> redpacketStack;

    private RedPacketTypeEnum type;

    private Long sponsorId;

    public WeixinRedPacket() {

    }

    public WeixinRedPacket(Long id, Long money, int number, RedPacketTypeEnum type, Long sponsorId) {
        super(id);
        Stack<Long> stack = new Stack<>();
        if (RedPacketTypeEnum.AVG.equals(type)) {
            Long avgMoney = money / number;
            //平均分配
            for (int i = 0; i < number; i++) {
                stack.push(avgMoney);
            }
        } else {
            throw new RuntimeException("unrealized");
        }
        RedPacketCreatedEvent event = new RedPacketCreatedEvent(stack);
        event.setAggregateId(id);
        event.setType(type);
        event.setSponsorId(sponsorId);
        super.applyNewEvent(event);
    }

    /**
     * 抢红包
     *
     * @param userId
     * @return
     */
    public int grabRedPackage(Long userId) {

        if (redpacketStack.size() == 0) {
            return 0;
        }

        if (map.get(userId) != null) {
            return -1;
        }

        super.applyNewEvent(new RedPacketGrabSucceedEvent(redpacketStack.pop(), userId));

        return 1;
    }

    private void apply(RedPacketGrabSucceedEvent event) {
        map.put(event.getUserId(), event.getMoney());
    }

    private void apply(RedPacketCreatedEvent event) {
        this.redpacketStack = event.getRedpacketStack();
        this.type = event.getType();
        this.sponsorId = event.getSponsorId();
        this.map = new HashMap<>();
    }

    @Override
    public long createSnapshootCycle() {
        return -1;
    }

    public Map<Long, Long> getMap() {
        return map;
    }

    public Stack<Long> getRedpacketStack() {
        return redpacketStack;
    }

    public RedPacketTypeEnum getType() {
        return type;
    }

    public Long getSponsorId() {
        return sponsorId;
    }
}
