package com.damon.cqrs.sample.trade_matching;

import com.damon.cqrs.sample.trade_matching.domain.aggregate.StockBuyOrder;

public class PriorityQueueTest {
    public static void main(String[] args) {
        java.util.PriorityQueue<StockBuyOrder> priorityQueue = new java.util.PriorityQueue<>((o1, o2) -> {
            // 先比较价格，如果价格相同，则比较创建时间
            int priceComparison = o2.getPrice().compareTo(o1.getPrice());
            if (priceComparison != 0) {
                // 高价格优先
                return priceComparison;
            } else {
                // 早创建的优先
                return o1.getCreateTime().compareTo(o2.getCreateTime());
            }
        });

//        priorityQueue.add(new StockBuyOrder(1L, 10L, 1, 1L));
//        priorityQueue.add(new StockBuyOrder(2L, 8L, 1, 1L));
//        priorityQueue.add(new StockBuyOrder(3L, 7L, 1, 1L));
//        priorityQueue.add(new StockBuyOrder(2L, 6L, 1, 1L));


        for (; ; ) {
            StockBuyOrder order = priorityQueue.poll();
            if (order == null) {
                break;
            }
            System.out.println(order);
        }

    }
}