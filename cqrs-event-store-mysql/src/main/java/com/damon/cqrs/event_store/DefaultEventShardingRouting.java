package com.damon.cqrs.event_store;

import com.damon.cqrs.store.IEventShardingRouting;

import java.util.Map;

/**
 * 聚合路由
 * <p>
 * 根据聚合根id选择相应的数据源，数据表
 *
 * @author xianpinglu
 */
public class DefaultEventShardingRouting implements IEventShardingRouting {

    @Override
    public Integer routeInstance(Long aggregateId, String aggregateType, Integer instanceNumber, Map<String, Object> shardingParams) {
        int hash = aggregateId.hashCode();
        hash = hash < 0 ? Math.abs(hash) : hash;
        return hash % instanceNumber;
    }

    @Override
    public Integer routeSharding(Long aggregateId, String aggregateType, Integer shardingNumber, Map<String, Object> shardingParams) {
        Long aggreId = aggregateId / 100;
        int hash = aggreId.hashCode();
        hash = hash < 0 ? Math.abs(hash) : hash;
        return hash % shardingNumber;
    }
}
