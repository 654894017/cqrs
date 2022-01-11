package com.damon.cqrs.sample.weixin;

public class RedPackage {
    int remainSize;
    double remainMoney;

    public void setRemainSize(int remainSize) {
        this.remainSize = remainSize;
    }

    public void setRemainMoney(double remainMoney) {
        this.remainMoney = remainMoney;
    }

    public int getRemainSize() {
        return remainSize;
    }

    public double getRemainMoney() {
        return remainMoney;
    }

    @Override
    public String toString() {
        final StringBuffer sb = new StringBuffer("RedPackage{");
        sb.append("remainSize=").append(remainSize);
        sb.append(", remainMoney=").append(remainMoney);
        sb.append('}');
        return sb.toString();
    }
}