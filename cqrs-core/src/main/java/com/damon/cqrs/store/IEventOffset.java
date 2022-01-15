package com.damon.cqrs.store;

import java.util.concurrent.CompletableFuture;

public interface IEventOffset {

    CompletableFuture<Boolean> updateEventOffset(long offsetId);

    CompletableFuture<Long> getEventOffset();

}
