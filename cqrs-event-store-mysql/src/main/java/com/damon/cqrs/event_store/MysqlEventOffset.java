package com.damon.cqrs.event_store;

import com.damon.cqrs.store.IEventOffset;
import org.springframework.jdbc.core.JdbcTemplate;

import javax.sql.DataSource;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CompletableFuture;

/**
 * 事件偏移位置存储
 * <p>
 * 用于记录event store已发送的事件的偏移位置
 *
 * @author xianpinglu
 */
public class MysqlEventOffset implements IEventOffset {

    private final String QUERY_EVENT_OFFSET = "SELECT id,event_offset_id,data_source_name,table_name FROM event_offset";

    private final String UPDATE_EVENT_OFFSET = "UPDATE event_offset SET event_offset_id = ? where id = ? ";

    private final JdbcTemplate jdbcTemplate;

    private Map<String, DataSource> dataSourceNameMap;
    public MysqlEventOffset(final List<DataSourceMapping> dataSourceMappings) {
        dataSourceNameMap = new HashMap<>();
        dataSourceMappings.forEach(mapping->{
            dataSourceNameMap.put(mapping.getDataSourceName(), mapping.getDataSource());
        });
        this.jdbcTemplate = new JdbcTemplate();
    }

    @Override
    public CompletableFuture<List<Map<String,Object>>> queryEventOffset() {
        try {
            List<Map<String,Object>> rows = jdbcTemplate.queryForList(QUERY_EVENT_OFFSET);
            return CompletableFuture.completedFuture(rows);
        } catch (Throwable e) {
            CompletableFuture<List<Map<String,Object>>> future = new CompletableFuture<>();
            future.completeExceptionally(e);
            return future;
        }
    }

    @Override
    public CompletableFuture<Boolean> updateEventOffset(long offsetId ,long id) {
        try {
            jdbcTemplate.update(UPDATE_EVENT_OFFSET, offsetId, id);
            return CompletableFuture.completedFuture(true);
        } catch (Throwable e) {
            CompletableFuture<Boolean> future = new CompletableFuture<>();
            future.completeExceptionally(e);
            return future;
        }
    }


}
