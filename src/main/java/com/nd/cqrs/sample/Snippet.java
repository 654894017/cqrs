package com.nd.cqrs.sample;

import java.util.concurrent.CompletableFuture;
import java.util.concurrent.locks.ReentrantLock;

public class Snippet {
        public static void main(String[] args) {
    //
    //        CompletableFuture<String> future = new CompletableFuture<>();
    //        CompletableFuture<String> future2 = future.thenApply(ss -> {
    //            return "adsf" + ss;
    //        });
    //        future.complete("3333333");
    //
    //        System.out.println(future2.join());
    //
    //        CompletableFuture<String> future3 = new CompletableFuture<>();
    //        CompletableFuture<String> future4 = future.thenCompose(ss -> {
    //            return CompletableFuture.completedFuture("adsf" + ss);
    //        });
    //        future3.complete("3333333");
    //
    //        System.out.println(future4.join());
    
            ReentrantLock lock = new ReentrantLock();
    
            new Thread(() -> {
                System.out.println(222222);
                lock.lock();
                System.out.println(1111111);
                lock.unlock();
            }).start();
            new Thread(() -> {
                lock.lock();
                test().thenAccept(str -> {
                    System.out.println(str);
                    try {
                        Thread.sleep(1111);
                    } catch (InterruptedException e) {
                        // TODO Auto-generated catch block
                        e.printStackTrace();
                    }
                    System.out.println(4444);
                });
                System.out.println(55555);
                lock.unlock();
                System.out.println(2);
            }).start();
            ;
    
        }
        public static CompletableFuture<String> test() {
            return CompletableFuture.completedFuture("asdfasdf");
        }

}

