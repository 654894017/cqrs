package com.damon.cqrs.event_store;

import com.damon.cqrs.IEventOffset;
import org.springframework.jdbc.core.JdbcTemplate;

import javax.sql.DataSource;
import java.util.List;
import java.util.concurrent.CompletableFuture;

/**
 * 事件偏移位置存储
 * <p>
 * 用于记录event store已发送的事件的偏移位置
 *
 * @author xianpinglu
 */
public class MysqlEventOffset implements IEventOffset {

    private final String QUERY_EVENT_OFFSET = "SELECT event_offset_id FROM event_offset";

    private final String UPDATE_EVENT_OFFSET = "UPDATE event_offset_id SET event_offset = ?";

    private final String INSERT_EVENT_OFFSET = "INSERT INTO event_offset(event_offset_id) VALUES (?)";

    private final JdbcTemplate jdbcTemplate;

    public MysqlEventOffset(final DataSource dataSource) {
        this.jdbcTemplate = new JdbcTemplate(dataSource);
    }

    @Override
    public CompletableFuture<Long> getEventOffset() {
        try {
            List<Long> rows = jdbcTemplate.queryForList(QUERY_EVENT_OFFSET, Long.class);
            Long offsetId = rows.isEmpty() ? 0L : rows.get(0);
            return CompletableFuture.completedFuture(offsetId);
        } catch (Throwable e) {
            CompletableFuture<Long> future = new CompletableFuture<>();
            future.completeExceptionally(e);
            return future;
        }
    }

    @Override
    public CompletableFuture<Boolean> updateEventOffset(long offsetId) {
        try {
            if (jdbcTemplate.update(UPDATE_EVENT_OFFSET, offsetId) == 0) {
                jdbcTemplate.update(INSERT_EVENT_OFFSET, offsetId);
            }
            return CompletableFuture.completedFuture(true);
        } catch (Throwable e) {
            CompletableFuture<Boolean> future = new CompletableFuture<>();
            future.completeExceptionally(e);
            return future;
        }
    }


}
