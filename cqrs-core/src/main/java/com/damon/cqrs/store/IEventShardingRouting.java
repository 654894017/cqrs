package com.damon.cqrs.store;

import javax.sql.DataSource;
import java.util.List;
import java.util.Map;

/**
 * 聚合路由
 * <p>
 * 根据聚合根id选择相应的数据源，数据表
 *
 * @author xianpinglu
 */
public interface IEventShardingRouting {
    /**
     * 路由数据源
     * @param aggregateId
     * @param aggregateType
     * @param dataSourceNumber
     * @param shardingParams
     * @return
     */
    Integer routeDataSource(Long aggregateId, String aggregateType, Integer dataSourceNumber,Map<String,Object> shardingParams);

    /**
     * 路由表
     *
     * @param aggregateType 聚合根类型
     * @param tableNumber   单个数据源表数量
     * @return
     */
    Integer routeTable(Long aggregateId, String aggregateType, Integer tableNumber, Map<String,Object> shardingParams);
}
