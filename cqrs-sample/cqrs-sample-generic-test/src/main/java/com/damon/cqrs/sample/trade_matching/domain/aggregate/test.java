package com.damon.cqrs.sample.trade_matching.domain.aggregate;

import java.util.*;
import java.util.concurrent.ConcurrentSkipListMap;
import java.util.concurrent.ConcurrentSkipListSet;

public class test {
    public static void main(String[] args) {
        TreeMap<Long, PriorityQueue<StockBuyOrder>> buyOrderMap = new TreeMap<>(Comparator.reverseOrder());
        System.out.println(buyOrderMap.firstEntry());
// 示例数据
        buyOrderMap.put(100L, new PriorityQueue<>());
        buyOrderMap.put(200L, new PriorityQueue<>());
        buyOrderMap.put(400L, new PriorityQueue<>());
        buyOrderMap.put(300L, new PriorityQueue<>());

        System.out.println(buyOrderMap);

        long targetPrice = 2300L;

// 找到所有价格大于 targetPrice 的条目
        SortedMap<Long, PriorityQueue<StockBuyOrder>> higherEntries = buyOrderMap.headMap(targetPrice, true);

        for (Map.Entry<Long, PriorityQueue<StockBuyOrder>> entry : higherEntries.entrySet()) {
            System.out.println("Price: " + entry.getKey() + ", Orders: " + entry.getValue());
        }


        ConcurrentSkipListSet<Integer> skipListSet = new ConcurrentSkipListSet<>();
        skipListSet.add(12);
        skipListSet.add(5);
        skipListSet.add(2);
        skipListSet.add(10);
        for (Integer value : skipListSet) {
            System.out.println(value); // 按顺序输出
        }
        ConcurrentSkipListMap skipListMap;

    }
}
