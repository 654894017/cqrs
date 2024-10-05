package com.damon.cqrs.sample.workflow3.workflow;

import com.google.common.collect.Lists;

import java.util.List;

public class Test {
    public static void main(String[] args) {
        List<List<Integer>> list = Lists.partition(Lists.newArrayList(1, 2, 3, 4, 5, 6, 7, 8, 9, 10), 3);

        System.out.println(list);

        List<List<Integer>> list2 = Lists.partition(Lists.newArrayList(), 3);

        System.out.println(list2);

    }
}
