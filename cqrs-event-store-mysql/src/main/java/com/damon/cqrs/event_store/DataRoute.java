package com.damon.cqrs.event_store;

import javax.sql.DataSource;
import java.util.List;

/**
 * 聚合路由
 * <p>
 * 根据聚合根id选择相应的数据源，数据表
 *
 * @author xianpinglu
 */
public class DataRoute {

    public static DataSource routeDataSource(String aggregateType, List<DataSource> dataSources) {
        int hash = aggregateType.hashCode();
        hash = hash < 0 ? Math.abs(hash) : hash;
        int size = dataSources.size();
        int index = hash % size;
        return dataSources.get(index);
    }

    public static Integer routeTable(Long aggregateId, Integer tableNumber) {
        int hash = aggregateId.hashCode();
        hash = hash < 0 ? Math.abs(hash) : hash;
        return hash % tableNumber;
    }


}
