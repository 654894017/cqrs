package com.damon.cqrs.sample.trade_matching.domain.aggregate;

import java.util.Comparator;
import java.util.Map;
import java.util.TreeMap;

class Order2 {
    double price;
    long createTime;
    int quantity;

    public Order2(double price, long createTime, int quantity) {
        this.price = price;
        this.createTime = createTime;
        this.quantity = quantity;
    }

    @Override
    public String toString() {
        return "Order{price=" + price + ", createTime=" + createTime + ", quantity=" + quantity + "}";
    }
}

public class TreeMapExample {
    public static void main(String[] args) {
        // 创建一个自定义比较器，先按价格比较，若价格相同则按创建时间比较
        Comparator<Order2> comparator = Comparator
                .comparingDouble((Order2 o) -> o.price)
                .thenComparing(o -> o.createTime);

        // 使用该比较器创建一个TreeMap
        TreeMap<Order2, String> orderMap = new TreeMap<>(comparator);

        // 添加订单
        orderMap.put(new Order2(10.5, 8L, 100), "Order 1");
        orderMap.put(new Order2(10.5, 7L, 150), "Order 2");
        orderMap.put(new Order2(11.0, 10, 200), "Order 3");
        orderMap.put(new Order2(9.8, 11L, 250), "Order 4");
        orderMap.put(new Order2(10.5, 6L, 300), "Order 5");

        // 遍历TreeMap并打印结果
        for (Map.Entry<Order2, String> entry : orderMap.entrySet()) {
            System.out.println(entry.getValue() + ": " + entry.getKey());
        }
    }
}