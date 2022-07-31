package com.damon.cqrs.utils;

import lombok.Data;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.locks.ReentrantLock;

@Data
public class AggregateLockUtils {

    public static List<ReentrantLock> locks = new ArrayList<>();

    private static int lockNumber = 4096;

    private static Object lock = new Object();

    public static ReentrantLock getLock(Long aggregateId) {
        if (locks.isEmpty()) {
            synchronized (lock) {
                if (locks.isEmpty()) {
                    for (int i = 0; i < lockNumber; i++) {
                        locks.add(new ReentrantLock(true));
                    }
                }
            }
        }
        int hash = aggregateId.hashCode();
        if (hash < 0) {
            hash = Math.abs(hash);
        }
        int index = hash % lockNumber;
        return locks.get(index);
    }

}
