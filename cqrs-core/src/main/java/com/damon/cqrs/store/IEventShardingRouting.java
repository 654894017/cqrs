package com.damon.cqrs.store;

import java.util.Map;

/**
 * 聚合路由
 *
 *  1个实例对应n个分片
 *
 * @author xianpinglu
 */
public interface IEventShardingRouting {
    /**
     * 路由到实例
     *
     * @param aggregateId
     * @param aggregateType
     * @param instanceNumber
     * @param shardingParams
     * @return
     */
    Integer routeInstance(Long aggregateId, String aggregateType, Integer instanceNumber, Map<String, Object> shardingParams);

    /**
     * 路由到分片
     *
     * @param aggregateType 聚合根类型
     * @param shardingNumber   分片数量
     * @return
     */
    Integer routeSharding(Long aggregateId, String aggregateType, Integer shardingNumber, Map<String, Object> shardingParams);
}
