package com.damon.cqrs.sample.trade_matching.domain.aggregate;

import java.util.Map;
import java.util.concurrent.ConcurrentSkipListMap;

public class SkipListMapExample {
    public static void main(String[] args) {
        ConcurrentSkipListMap<Integer, String> map = new ConcurrentSkipListMap<>();

        map.put(3, "Three");
        map.put(1, "One");
        map.put(2, "Two");
        map.put(5, "Five");
        map.put(4, "Four");

        // 遍历 ConcurrentSkipListMap，按自然顺序（升序）排列
        for (Map.Entry<Integer, String> entry : map.entrySet()) {
            System.out.println(entry.getKey() + ": " + entry.getValue());
        }
    }
}