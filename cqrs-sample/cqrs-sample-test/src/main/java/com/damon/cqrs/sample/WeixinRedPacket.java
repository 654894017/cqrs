package com.damon.cqrs.sample;

import com.damon.cqrs.domain.Aggregate;

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
     * key 用户id   value  抢到的金额
     */
    private final Map<Long, Long> map;

    private Stack<Long> redpacketStack;

    private RedPacketTypeEnum type;

    private Long sponsorId;

    public WeixinRedPacket(Long id, Long money, int number, RedPacketTypeEnum type, Long sponsorId) {
        super(id);
        map = new HashMap<>(number);
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
        RedPacketGrabCreatedEvent event = new RedPacketGrabCreatedEvent(stack);
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

        super.applyNewEvent(new RedPacketGrabSucceedEvent(redpacketStack.peek(), userId));

        return 1;
    }

    private void apply(RedPacketGrabSucceedEvent event) {
        map.put(event.getUserId(), redpacketStack.pop());
    }

    private void apply(RedPacketGrabCreatedEvent event) {
        this.redpacketStack = event.getRedpacketStack();
        this.type = event.getType();
        this.sponsorId = event.getSponsorId();
    }

    @Override
    public long createSnapshootCycle() {
        return -1;
    }

}
