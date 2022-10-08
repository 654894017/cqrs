package com.damon.cqrs.utils;

import com.damon.cqrs.domain.Command;
import com.damon.cqrs.exception.AggregateCommandConflictException;
import com.damon.cqrs.exception.AggregateEventConflictException;
import lombok.extern.slf4j.Slf4j;

import java.util.concurrent.CompletionException;
import java.util.function.Supplier;

/**
 * 聚合更新冲突重试工具类
 * <p>
 * 当集群扩容时，有可能导致一个聚合根在多个服务器的问题。
 * <p>
 * 当出现这个情况时，会有可能出现聚合更新version冲突的问题，我们需要捕获AggregateEventConflictException异常，然后在client发起重试解决这个问题。
 *
 * @author xianping_lu
 */
@Slf4j
public class AggregateConflictRetryUtils {

    public static <R> R invoke(Command command, Supplier<R> supplier) {
        return invoke(command, supplier, 2);
    }

    public static <R> R invoke(Command command, Supplier<R> supplier, int retryNumber) {
        for (int i = 0; i < retryNumber; i++) {
            try {
                return supplier.get();
            } catch (CompletionException e) {
                if (e.getCause() instanceof AggregateEventConflictException) {
                    AggregateEventConflictException ex = (AggregateEventConflictException) e.getCause();
                    log.error("aggregate update conflict, aggregate id : {}, type : {}.", ex.getAggregateId(), ex.getAggregateType(), e);
                    if (i == (retryNumber - 1)) {
                        throw ex;
                    }
                } else if (e.getCause() instanceof AggregateCommandConflictException) {
                    AggregateCommandConflictException ex = (AggregateCommandConflictException) e.getCause();
                    long commandId = ex.getCommandId();
                    log.error("aggregate update conflict, aggregate id : {}, type : {}, command id : {}.", ex.getAggregateId(), ex.getAggregateType(), commandId, e);
                    if (commandId == command.getCommandId()) {
                        throw ex;
                    }
                } else {
                    throw e;
                }
            }
        }
        throw new AggregateEventConflictException("aggregate update conflict, retry failed.");
    }

}
