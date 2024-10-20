package com.damon.cqrs.sample.trade_matching.domain.aggregate;

import java.time.LocalDateTime;

public class Order {
    private long id;  // 订单ID
    private long price;  // 价格（市价单为0）
    private int quantity;  // 数量
    private LocalDateTime createTime;  // 创建时间
    private boolean isMarketOrder;  // 是否为市价单

    public Order(long id, long price, int quantity, LocalDateTime createTime, boolean isMarketOrder) {
        this.id = id;
        this.price = price;
        this.quantity = quantity;
        this.createTime = createTime;
        this.isMarketOrder = isMarketOrder;
    }

    public long getId() {
        return id;
    }

    public long getPrice() {
        return price;
    }

    public int getQuantity() {
        return quantity;
    }

    public void setQuantity(int quantity) {
        this.quantity = quantity;
    }

    public LocalDateTime getCreateTime() {
        return createTime;
    }

    public boolean isMarketOrder() {
        return isMarketOrder;
    }

    @Override
    public String toString() {
        return "Order{id=" + id + ", price=" + price + ", quantity=" + quantity + ", createTime=" + createTime + "}";
    }
}