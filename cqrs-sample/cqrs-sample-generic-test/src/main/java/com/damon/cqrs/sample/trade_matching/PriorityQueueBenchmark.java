package com.damon.cqrs.sample.trade_matching;

import java.util.PriorityQueue;

public class PriorityQueueBenchmark {
    public static void main(String[] args) {
        PriorityQueue<Integer> priorityQueue = new PriorityQueue<>();
        int totalOperations = 1_000_000; // 总操作数
        long startTime = System.nanoTime();

        // 插入操作
        for (int i = 0; i < totalOperations; i++) {
            priorityQueue.offer((int) (Math.random() * totalOperations));
        }

        // 删除操作
        for (int i = 0; i < totalOperations; i++) {
            priorityQueue.poll();
        }

        long endTime = System.nanoTime();

        System.out.println("PriorityQueue benchmark completed in " + (endTime - startTime) / 1_000_000 + " ms.");

    }
}