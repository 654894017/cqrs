package com.damon.cqrs.sample.trade_matching.api.cmd;


import com.google.common.collect.Lists;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class Test {
    public static void main(String[] args) {
        Map<String, List<String>> map = new HashMap<>();
        map.put("a1", Lists.newArrayList("1", "2"));
        map.put("a2", Lists.newArrayList("1", "2"));
        map.put("a3", Lists.newArrayList("1", "2"));

        for (Map.Entry<String, List<String>> entry : map.entrySet()) {
            map.get("a2").remove("1");
            map.get("a3").remove("2");
            System.out.println(entry);
        }


    }
}
