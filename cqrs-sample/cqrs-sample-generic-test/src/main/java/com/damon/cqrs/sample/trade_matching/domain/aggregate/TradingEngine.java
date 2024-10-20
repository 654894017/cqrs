package com.damon.cqrs.sample.trade_matching.domain.aggregate;

import java.time.LocalDateTime;
import java.util.Comparator;
import java.util.PriorityQueue;
import java.util.TreeMap;

public class TradingEngine {

    // 用于存储限价买单（价格倒序，最高价优先）
    private TreeMap<Long, PriorityQueue<Order>> buyLimitOrders = new TreeMap<>(Comparator.reverseOrder());
    // 用于存储限价卖单（价格正序，最低价优先）
    private TreeMap<Long, PriorityQueue<Order>> sellLimitOrders = new TreeMap<>();
    // 市价买单队列，按时间先后排序
    private PriorityQueue<Order> buyMarketOrders = new PriorityQueue<>(Comparator.comparing(Order::getCreateTime));
    // 市价卖单队列，按时间先后排序
    private PriorityQueue<Order> sellMarketOrders = new PriorityQueue<>(Comparator.comparing(Order::getCreateTime));

    public static void main(String[] args) {
        TradingEngine engine = new TradingEngine();

        // 提交一些订单
        engine.submitOrder(new Order(1, 10, 100, LocalDateTime.now().minusMinutes(5), false));  // 限价买单
        engine.submitOrder(new Order(2, 9, 200, LocalDateTime.now().minusMinutes(3), false));   // 限价卖单
        engine.submitOrder(new Order(3, 0, 150, LocalDateTime.now(), true));                   // 市价买单
        engine.submitOrder(new Order(4, 0, 100, LocalDateTime.now(), true));                   // 市价卖单
    }

    // 提交订单
    public void submitOrder(Order order) {
        if (order.isMarketOrder()) {
            if (order.getPrice() == 0) {
                // 市价买单
                buyMarketOrders.add(order);
                processMarketOrders();
            } else {
                // 市价卖单
                sellMarketOrders.add(order);
                processMarketOrders();
            }
        } else {
            // 限价买单
            if (order.getPrice() > 0) {
                buyLimitOrders.putIfAbsent(order.getPrice(), new PriorityQueue<>(Comparator.comparing(Order::getCreateTime)));
                buyLimitOrders.get(order.getPrice()).add(order);
            } else {
                // 限价卖单
                sellLimitOrders.putIfAbsent(order.getPrice(), new PriorityQueue<>(Comparator.comparing(Order::getCreateTime)));
                sellLimitOrders.get(order.getPrice()).add(order);
            }
            processLimitOrders();
        }
    }

    // 市价单匹配
    private void processMarketOrders() {
        while (!buyMarketOrders.isEmpty() && !sellLimitOrders.isEmpty()) {
            Order buyOrder = buyMarketOrders.poll();  // 获取市价买单
            matchOrder(buyOrder, sellLimitOrders.firstEntry().getValue().peek()); // 匹配最低价卖单
        }

        while (!sellMarketOrders.isEmpty() && !buyLimitOrders.isEmpty()) {
            Order sellOrder = sellMarketOrders.poll();  // 获取市价卖单
            matchOrder(sellOrder, buyLimitOrders.firstEntry().getValue().peek()); // 匹配最高价买单
        }
    }

    // 限价单匹配
    private void processLimitOrders() {
        while (!buyLimitOrders.isEmpty() && !sellLimitOrders.isEmpty()) {
            long highestBuyPrice = buyLimitOrders.firstKey();
            long lowestSellPrice = sellLimitOrders.firstKey();

            if (highestBuyPrice >= lowestSellPrice) {
                // 如果最高买价大于等于最低卖价，则匹配
                Order buyOrder = buyLimitOrders.firstEntry().getValue().peek();
                Order sellOrder = sellLimitOrders.firstEntry().getValue().peek();
                matchOrder(buyOrder, sellOrder);
            } else {
                // 如果无法匹配则停止
                break;
            }
        }
    }

    // 订单匹配逻辑
    private void matchOrder(Order buyOrder, Order sellOrder) {
        if (buyOrder == null || sellOrder == null) {
            return;
        }

        int tradedQuantity = Math.min(buyOrder.getQuantity(), sellOrder.getQuantity());
        buyOrder.setQuantity(buyOrder.getQuantity() - tradedQuantity);
        sellOrder.setQuantity(sellOrder.getQuantity() - tradedQuantity);

        System.out.println("Matched " + tradedQuantity + " shares at price " + sellOrder.getPrice());

        // 移除完成的订单
        if (buyOrder.getQuantity() == 0) {
            buyLimitOrders.get(buyOrder.getPrice()).poll();
            if (buyLimitOrders.get(buyOrder.getPrice()).isEmpty()) {
                buyLimitOrders.remove(buyOrder.getPrice());
            }
        }

        if (sellOrder.getQuantity() == 0) {
            sellLimitOrders.get(sellOrder.getPrice()).poll();
            if (sellLimitOrders.get(sellOrder.getPrice()).isEmpty()) {
                sellLimitOrders.remove(sellOrder.getPrice());
            }
        }
    }
}