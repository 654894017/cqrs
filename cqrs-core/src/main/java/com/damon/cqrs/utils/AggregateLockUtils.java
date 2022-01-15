package com.damon.cqrs.utils;

import lombok.Data;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.locks.ReentrantLock;

@Data
public class AggregateLockUtils {

    private static List<ReentrantLock> locks = new ArrayList<>();

    private static int lockNumber = 1024;

    private static Object lock = new Object();

    public static ReentrantLock getLock(long aggregateId) {
        if (locks.isEmpty()) {
            synchronized (lock) {
                if (locks.isEmpty()) {
                    for (int i = 0; i < lockNumber; i++) {
                        locks.add(new ReentrantLock(true));
                    }
                }
            }
        }
        int index = (int) (Math.abs(aggregateId) % lockNumber);
        return locks.get(index);
    }

}
