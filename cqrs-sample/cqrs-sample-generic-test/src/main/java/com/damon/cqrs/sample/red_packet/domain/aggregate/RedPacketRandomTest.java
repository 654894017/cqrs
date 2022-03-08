package com.damon.cqrs.sample.red_packet.domain.aggregate;

import java.util.ArrayList;
import java.util.List;
import java.util.Random;

public class RedPacketRandomTest {
    public static double getRandomMoney(RedPackage _redPackage) {
        // remainSize 剩余的红包数量
        // remainMoney 剩余的钱
        if (_redPackage.remainSize == 1) {
            _redPackage.remainSize--;
            return (double) Math.round(_redPackage.remainMoney * 100) / 100;
        }
        Random r = new Random();
        double min = 0.01; //
        double max = _redPackage.remainMoney / _redPackage.remainSize * 2;
        double money = r.nextDouble() * max;
        money = money <= min ? 0.01 : money;
        money = Math.floor(money * 100) / 100;
        _redPackage.remainSize--;
        _redPackage.remainMoney -= money;
        return money;
    }

    /**
     * 每次产生一个红包
     *
     * @param money 总金额
     * @param count 数量
     * @return
     */
    public static List<Double> listRedPacket(double money, int count) {
        RedPackage redPackage = new RedPackage();
        List<Double> list = new ArrayList<>();
        double mon = 0;
        double mony = 0;
        for (int i = 0; i < count; i++) {
            if (i == 0) {
                redPackage.setRemainMoney(money);
                redPackage.setRemainSize(count);
                mon = getRandomMoney(redPackage);
                mony += mon;
            } else {
                redPackage.setRemainMoney(money - mony);
                redPackage.setRemainSize(count - i);
                mon = getRandomMoney(redPackage);
                mony += mon;
            }
            list.add(mon);
        }
        return list;
    }

    public static void main(String[] args) {

        for (int i = 0; i < 10; i++) {
            List<Double> list2 = listRedPacket(10, 3);
            System.out.println(list2);
        }
    }

    public static class RedPackage {

        public int remainSize;

        public double remainMoney;

        public int getRemainSize() {
            return remainSize;
        }

        public void setRemainSize(int remainSize) {
            this.remainSize = remainSize;
        }

        public double getRemainMoney() {
            return remainMoney;
        }

        public void setRemainMoney(double remainMoney) {
            this.remainMoney = remainMoney;
        }


    }

}
