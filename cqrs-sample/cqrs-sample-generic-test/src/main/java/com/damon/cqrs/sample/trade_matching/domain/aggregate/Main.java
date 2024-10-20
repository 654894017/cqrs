package com.damon.cqrs.sample.trade_matching.domain.aggregate;

import java.math.BigDecimal;
import java.util.TreeMap;

// 订单类
class MergeOrder {
    private final String type; // 订单类型：买单或卖单
    private final BigDecimal price; // 限价单的价格
    private int quantity; // 订单数量

    public MergeOrder(String type, BigDecimal price, int quantity) {
        this.type = type;
        this.price = price;
        this.quantity = quantity;
    }

    public String getType() {
        return type;
    }

    public BigDecimal getPrice() {
        return price;
    }

    public int getQuantity() {
        return quantity;
    }

    public void reduceQuantity(int amount) {
        this.quantity -= amount;
    }

    @Override
    public String toString() {
        return type + " Order [Price: " + price + ", Quantity: " + quantity + "]";
    }
}

// 交易撮合引擎
class MatchingEngine {
    private final TreeMap<BigDecimal, MergeOrder> buyLimitPrice; // 买入限价单存储

    public MatchingEngine() {
        this.buyLimitPrice = new TreeMap<>();
    }

    // 添加买入限价单
    public void addBuyLimitOrder(MergeOrder order) {
        buyLimitPrice.put(order.getPrice(), order);
    }

    // 执行撮合
    public void matchOrders(MergeOrder marketOrder) {
        // 市价买单，获取最高价格的限价买单
        BigDecimal highestPrice = buyLimitPrice.lastKey();

        // 检查是否可以撮合
        if (marketOrder.getPrice().compareTo(highestPrice) >= 0) {
            MergeOrder matchedOrder = buyLimitPrice.get(highestPrice);
            System.out.println("Matching: " + marketOrder + " with " + matchedOrder);

            // 更新订单数量（假设每次撮合1个单位）
            matchedOrder.reduceQuantity(1);

            // 移除已成交的限价单
            if (matchedOrder.getQuantity() == 0) {
                buyLimitPrice.remove(highestPrice);
            }
        } else {
            System.out.println("No match for: " + marketOrder);
        }
    }
}

// 主类
public class Main {
    public static void main(String[] args) {
        MatchingEngine engine = new MatchingEngine();

        // 添加买入限价单
        engine.addBuyLimitOrder(new MergeOrder("Buy", BigDecimal.valueOf(10.5), 5));
        engine.addBuyLimitOrder(new MergeOrder("Buy", BigDecimal.valueOf(10.0), 3));

        // 创建市价买单
        MergeOrder marketOrder = new MergeOrder("Market Buy", BigDecimal.valueOf(10.2), 1);

        // 执行撮合
        engine.matchOrders(marketOrder);
    }
}