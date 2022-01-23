package com.damon.cqrs.sample.train.domain;

import java.util.ArrayList;
import java.util.BitSet;
import java.util.List;

public class BitSetTest {
    public static void main(String[] args) {
        BitSet set = new BitSet();
        set.set(1);
        set.set(2);
        set.set(4);

        BitSet set2 = new BitSet();
        set2.set(1);
        set2.set(2);
        set2.set(3);

        List<BitSet> list = new ArrayList<>();
        list.add(set);
        list.add(set2);
//
//        list.stream().map(ss->ss).reduce(new BitSet(),(s)->{
//           //return s.and();
//        });


    }
}
