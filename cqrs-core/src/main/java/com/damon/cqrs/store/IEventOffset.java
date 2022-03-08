package com.damon.cqrs.store;

import java.util.List;
import java.util.Map;
import java.util.concurrent.CompletableFuture;

public interface IEventOffset {

    CompletableFuture<Boolean> updateEventOffset(String dataSourceName, long offsetId, long id);

    CompletableFuture<List<Map<String, Object>>> queryEventOffset();

}
