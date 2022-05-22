package com.damon.cqrs.event_store;

import com.damon.cqrs.store.IEventShardingRoute;

import javax.sql.DataSource;
import java.util.List;

/**
 * 聚合路由
 * <p>
 * 根据聚合根id选择相应的数据源，数据表
 *
 * @author xianpinglu
 */
public class DefaultEventShardingRoute implements IEventShardingRoute {

    @Override
    public DataSource routeDataSource(Long aggregateId, String aggregateType, List<DataSource> dataSources) {
        int hash = aggregateId.hashCode();
        hash = hash < 0 ? Math.abs(hash) : hash;
        int size = dataSources.size();
        return dataSources.get(hash % size);
    }

    @Override
    public Integer routeTable(Long aggregateId, String aggregateType, Integer tableNumber) {
        Long aggreId = aggregateId / 1000;
        int hash = aggreId.hashCode();
        hash = hash < 0 ? Math.abs(hash) : hash;
        return hash % tableNumber;
    }
}
