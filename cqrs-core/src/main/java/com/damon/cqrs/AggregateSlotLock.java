package com.damon.cqrs;

import lombok.Data;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.locks.ReentrantLock;

/**
 * 聚合根锁
 *
 *
 *
 */
@Data
public class AggregateSlotLock {
    private final List<ReentrantLock> locks = new ArrayList<>();
    private final int lockNumber;

    public AggregateSlotLock(int lockNumber) {
        this.lockNumber = lockNumber;
        for (int i = 0; i < lockNumber; i++) {
            locks.add(new ReentrantLock(true));
        }
    }

    public ReentrantLock getLock(Long aggregateId) {
        int hash = aggregateId.hashCode();
        if (hash < 0) {
            hash = Math.abs(hash);
        }
        int index = hash % lockNumber;
        return locks.get(index);
    }

}
