package com.damon.cqrs.utils;

import java.util.Map;
@FunctionalInterface
public interface AggregateRecoveryFunction {

    void callback(Long aggregateId, String aggregateTypeName, Map<String, Object> shardingParams);

}
