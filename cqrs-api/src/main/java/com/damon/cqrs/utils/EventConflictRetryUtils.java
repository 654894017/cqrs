package com.damon.cqrs.utils;

import java.util.concurrent.CompletionException;
import java.util.function.Supplier;

import com.damon.cqrs.exception.AggregateEventConflictException;

import lombok.extern.slf4j.Slf4j;

/**
 * 
 * 聚合更新冲突重试工具类
 * 
 * 当集群扩容缩容时，有可能导致一个聚合根在多个服务器的问题。
 * 
 * 当出现这个情况时，会有可能出现聚合更新version冲突的问题，我们需要捕获AggregateEventConflictException异常，然后在client发起重试解决这个问题。
 * 
 * 
 * @author xianping_lu
 *
 */
@Slf4j
public class EventConflictRetryUtils {

    public static <R> R invoke(Supplier<R> supplier) {
        return invoke(supplier, 2);
    }

    public static <R> R invoke(Supplier<R> supplier, int retryNumber) {
        for (int i = 0; i < retryNumber; i++) {
            try {
                return supplier.get();
            } catch (CompletionException e) {
                if (e.getCause() instanceof AggregateEventConflictException) {
                    AggregateEventConflictException ex = (AggregateEventConflictException) e.getCause();
                    log.error("aggregate update conflict, aggregate id : {}, type : {}.", ex.getAggregateId(), ex.getAggregateType(), ex);
                    if (i == (retryNumber - 1)) {
                        throw e;
                    }
                }
            }
        }
        throw new AggregateEventConflictException("aggregate update conflict, retry failture.");
    }

}
