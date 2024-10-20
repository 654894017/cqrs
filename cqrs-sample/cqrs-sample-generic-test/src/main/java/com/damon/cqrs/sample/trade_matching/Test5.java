package com.damon.cqrs.sample.trade_matching;

import java.util.concurrent.CompletableFuture;

public class Test5 {
    public static void main(String[] args) throws InterruptedException {

        CompletableFuture<Boolean> future = new CompletableFuture<>();
        future.completeExceptionally(new RuntimeException("11"));
        System.out.println(future.join());

    }
}
