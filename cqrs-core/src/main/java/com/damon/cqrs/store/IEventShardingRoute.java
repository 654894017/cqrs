package com.damon.cqrs.store;

import javax.sql.DataSource;
import java.util.List;

/**
 * 聚合路由
 * <p>
 * 根据聚合根id选择相应的数据源，数据表
 *
 * @author xianpinglu
 */
public interface IEventShardingRoute {
    /**
     *
     * 路由数据源
     *
     * @param aggregateId   聚合根id
     * @param aggregateType 聚合根类型
     * @param dataSources   数据源列表
     * @return
     */
    DataSource routeDataSource(Long aggregateId, String aggregateType, List<DataSource> dataSources);

    /**
     * 路由表
     *
     * @param aggregateType 聚合根类型
     * @param tableNumber   单个数据源表数量
     * @return
     */
    Integer routeTable(Long aggregateId, String aggregateType, Integer tableNumber);
}
