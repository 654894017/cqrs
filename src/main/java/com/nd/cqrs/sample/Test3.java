package com.nd.cqrs.sample;

import java.util.concurrent.CompletableFuture;

public class Test3 {
    public static void main(String[] args) throws InterruptedException {
        CompletableFuture<String> future = CompletableFuture.completedFuture("aaaaaaaaaaaaa");
        future.thenApply(result->{
            try {
                Thread.sleep(1000);
            } catch (InterruptedException e) {
                // TODO Auto-generated catch block
                e.printStackTrace();
            }
            System.out.println(333234);
            return "abc"+result;
        });
        
        
        System.out.println(3333333);
        
        
    }
}
