package com.damon.cqrs.sample.red_packet.domain.aggregate;

import com.damon.cqrs.domain.AggregateRoot;
import com.damon.cqrs.sample.red_packet.api.command.RedPacketCreateCommand;
import com.damon.cqrs.sample.red_packet.api.command.RedPacketGrabCommand;
import com.damon.cqrs.sample.red_packet.api.event.RedPacketCreatedEvent;
import com.damon.cqrs.sample.red_packet.api.event.RedPacketGrabSucceedEvent;

import java.util.HashMap;
import java.util.Map;
import java.util.Random;
import java.util.Stack;

/**
 * 微信红包聚合根
 *
 * @author xianpinglu
 */
public class WeixinRedPacket extends AggregateRoot {
    /**
     *
     */
    private static final long serialVersionUID = -1820552753606925363L;
    /**
     * key 用户id   value  抢到的金额
     */
    private Map<Long, Double> map;
    /**
     * 待抢红包堆栈
     */
    private Stack<Double> redpacketStack;
    /**
     * 红包发起人
     */
    private Long sponsorId;
    /**
     * 红包金额
     **/
    private double money;
    /**
     * 红包个数
     */
    private int size;

    public WeixinRedPacket() {

    }

    /**
     * 创建红包
     *
     * @param command
     */
    public WeixinRedPacket(RedPacketCreateCommand command) {
        super(command.getAggregateId());
        Stack<Double> stack = generateRandomMoneyStack(command.getMoney(), command.getNumber());
        RedPacketCreatedEvent event = new RedPacketCreatedEvent(stack);
        event.setAggregateId(command.getAggregateId());
        event.setSponsorId(sponsorId);
        event.setMoney(command.getMoney());
        event.setSize(command.getNumber());
        super.applyNewEvent(event);
    }

    public static void main(String args[]) {
        double d = 100.975;
        float f = -90.2f;

        System.out.println(Math.floor(d));
        System.out.println(Math.floor(f));

        System.out.println(Math.ceil(d));
        System.out.println(Math.ceil(f));
    }

    /**
     * 抢红包
     *
     * @param command
     * @return
     */
    public int grabRedPackage(RedPacketGrabCommand command) {
        //红包已抢完
        if (redpacketStack.size() == 0) {
            return 0;
        }
        //用户已抢过红包
        if (map.get(command.getUserId()) != null) {
            return -1;
        }
        //抢红包成功
        super.applyNewEvent(new RedPacketGrabSucceedEvent(redpacketStack.peek(), command.getUserId()));
        return 1;
    }

    private void apply(RedPacketCreatedEvent event) {
        this.redpacketStack = event.getRedpacketStack();
        this.sponsorId = event.getSponsorId();
        this.map = new HashMap<>();
        this.size = event.getSize();
        this.money = event.getMoney();
    }

    private void apply(RedPacketGrabSucceedEvent event) {
        map.put(event.getUserId(), redpacketStack.pop());
    }

    @Override
    public long createSnapshotCycle() {
        return -1;
    }

    public Map<Long, Double> getMap() {
        return map;
    }

    public Stack<Double> getRedpacketStack() {
        return redpacketStack;
    }

    public Long getSponsorId() {
        return sponsorId;
    }

    /**
     * 随件根据指定金额创建指定个数的红包列表
     *
     * @param totalMoney
     * @param size
     * @return
     */
    private Stack<Double> generateRandomMoneyStack(Double totalMoney, int size) {
        Stack<Double> stack = new Stack<>();
        // remainSize 剩余的红包数量 , remainMoney 剩余的钱
        Double remainMoney = totalMoney;
        int remainSize = size;
        for (int i = 0; i < size; i++) {
            if (remainSize == 1) {
                remainSize--;
                stack.add((double) Math.round(remainMoney * 100) / 100);
            } else {
                Random r = new Random();
                double min = 0.01;
                double max = remainMoney / remainSize * 2;
                double money = r.nextDouble() * max;
                money = money <= min ? 0.01 : money;
                money = Math.floor(money * 100) / 100;
                remainSize--;
                remainMoney -= money;
                stack.add(money);
            }
        }
        return stack;
    }
}
