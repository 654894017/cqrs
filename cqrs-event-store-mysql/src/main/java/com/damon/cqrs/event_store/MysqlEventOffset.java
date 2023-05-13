package com.damon.cqrs.event_store;

import com.damon.cqrs.store.IEventOffset;
import org.apache.commons.dbutils.QueryRunner;
import org.apache.commons.dbutils.handlers.MapHandler;
import org.apache.commons.dbutils.handlers.MapListHandler;

import javax.sql.DataSource;
import java.util.*;
import java.util.concurrent.CompletableFuture;

/**
 * 事件偏移位置存储
 * <p>
 * 用于记录event store已发送的事件的偏移位置
 *
 * @author xianping_lu
 */
public class MysqlEventOffset implements IEventOffset {

    private final String QUERY_EVENT_OFFSET = "SELECT id, event_offset_id, data_source_name, table_name FROM event_offset";

    private final String UPDATE_EVENT_OFFSET = "UPDATE event_offset SET event_offset_id = ? where id = ? ";

    private final Map<String, DataSource> dataSourceNameMap;

    public MysqlEventOffset(final List<DataSourceMapping> dataSourceMappings) {
        dataSourceNameMap = new HashMap<>();
        dataSourceMappings.forEach(mapping -> {
            dataSourceNameMap.put(mapping.getDataSourceName(), mapping.getDataSource());
        });
    }

    @Override
    public CompletableFuture<List<Map<String, Object>>> queryEventOffset() {
        try {
            List<Map<String, Object>> list = new ArrayList<>();
            Set<String> set = dataSourceNameMap.keySet();
            for (String name: set){
                DataSource dataSource = dataSourceNameMap.get(name);
                QueryRunner queryRunner = new QueryRunner(dataSource);
                List<Map<String, Object>> rows = queryRunner.query(QUERY_EVENT_OFFSET, new MapListHandler());
                list.addAll(rows);
            }
            return CompletableFuture.completedFuture(list);
        } catch (Throwable e) {
            CompletableFuture<List<Map<String, Object>>> future = new CompletableFuture<>();
            future.completeExceptionally(e);
            return future;
        }
    }

    @Override
    public CompletableFuture<Boolean> updateEventOffset(String dataSourceName, long offsetId, long id) {
        try {
            DataSource dataSource = dataSourceNameMap.get(dataSourceName);
            QueryRunner queryRunner = new QueryRunner(dataSource);
            queryRunner.update(UPDATE_EVENT_OFFSET, offsetId, id);
            return CompletableFuture.completedFuture(true);
        } catch (Throwable e) {
            CompletableFuture<Boolean> future = new CompletableFuture<>();
            future.completeExceptionally(e);
            return future;
        }
    }
}
