package com.damon.cqrs.store;

import java.sql.BatchUpdateException;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

import org.apache.commons.lang3.StringUtils;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.jdbc.core.JdbcTemplate;

import com.alibaba.fastjson.JSONArray;
import com.alibaba.fastjson.JSONObject;
import com.damon.cqrs.AggregateEventAppendResult;
import com.damon.cqrs.AggregateGroup;
import com.damon.cqrs.DomainEventStream;
import com.damon.cqrs.EventAppendStatus;
import com.damon.cqrs.EventSendingContext;
import com.damon.cqrs.IEventStore;
import com.damon.cqrs.domain.Aggregate;
import com.damon.cqrs.domain.Event;
import com.damon.cqrs.exception.AggregateCommandConflictException;
import com.damon.cqrs.exception.AggregateEventConflictException;
import com.damon.cqrs.exception.EventStoreException;
import com.damon.cqrs.utils.ReflectUtils;
import com.google.common.collect.Lists;

public class MysqlEventStore implements IEventStore {

    private final String QUERY_AGGREGATE_EVENTS = "SELECT events FROM event_stream WHERE aggregate_root_id = ?  and  version >= ? and version <= ? ORDER BY version asc";

    private final String INSERT_AGGREGATE_EVENTS = "INSERT INTO event_stream ( aggregate_root_type_name ,  aggregate_root_id ,  version ,  command_id ,  gmt_create ,  events ) VALUES (?, ?, ?, ?, ?, ?)";

    private final String QUERY_AGGREGATE_WAITING_SEND_EVENTS = "SELECT * FROM event_stream  WHERE ID > ? ORDER BY  id ASC  LIMIT 20000";

    private final String QUERY_EVENT_OFFSET = "SELECT event_offset_id FROM event_offset";

    private final String UPDATE_EVENT_OFFSET = "UPDATE event_offset_id SET event_offset = ?";

    private final String INSERT_EVENT_OFFSET = "INSERT INTO event_offset(event_offset_id) VALUES (?)";

    private final String sqlState = "23000";

    private final String eventTableVersionUniqueIndexName = "uk_aggregate_root_id_version";

    private final String eventTableCommandIdUniqueIndexName = "uk_aggregate_root_id_command_id";

    private final Pattern PATTERN_MYSQL = Pattern.compile("^Duplicate entry '.*-(.*)' for key");

    private final JdbcTemplate jdbcTemplate;

    public MysqlEventStore(final JdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }


    @Override
    public CompletableFuture<Long> getEventOffset() {
        try {
            List<Long> rows = jdbcTemplate.queryForList(QUERY_EVENT_OFFSET, Long.class);
            Long offsetId = rows.isEmpty() ? 0 : rows.get(0);
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
            if (jdbcTemplate.update(UPDATE_EVENT_OFFSET, new Object[] { offsetId }) == 0) {
                jdbcTemplate.update(INSERT_EVENT_OFFSET, new Object[] { offsetId });
            }
            return CompletableFuture.completedFuture(true);
        } catch (Throwable e) {
            CompletableFuture<Boolean> future = new CompletableFuture<>();
            future.completeExceptionally(e);
            return future;
        }
    }

    @Override
    public CompletableFuture<List<EventSendingContext>> queryWaitingSendEvents(long offsetId) {
        try {
            List<Map<String, Object>> rows = jdbcTemplate.queryForList(QUERY_AGGREGATE_WAITING_SEND_EVENTS, new Object[] { offsetId });
            List<EventSendingContext> sendingContexts = rows.stream().map(map -> {
                String aggregateId = (String) map.get("aggregate_root_id");
                String aggregateType = (String) map.get("aggregate_root_type_name");
                String eventJson = (String) map.get("events");
                Long id = (Long) map.get("id");
                JSONArray array = JSONArray.parseArray(eventJson);
                List<Event> events = new ArrayList<>(array.size());
                array.forEach(object -> {
                    JSONObject jsonObject = (JSONObject) object;
                    String eventType = jsonObject.getString("eventType");
                    Event event = (Event) JSONObject.parseObject(jsonObject.toString(), ReflectUtils.getClass(eventType));
                    events.add(event);
                });
                return EventSendingContext.builder().offsetId(id).aggregateId(Long.parseLong(aggregateId)).aggregateType(aggregateType).events(events).build();
            }).collect(Collectors.toList());
            return CompletableFuture.completedFuture(sendingContexts);
        } catch (Throwable e) {
            CompletableFuture<List<EventSendingContext>> future = new CompletableFuture<>();
            future.completeExceptionally(e);
            return future;
        }
    }

    @Override
    public CompletableFuture<List<AggregateEventAppendResult>> store(Map<AggregateGroup, List<DomainEventStream>> map) {
        List<AggregateEventAppendResult> resultList = map.keySet().stream().map(group -> {
            List<Object[]> batchParams = new ArrayList<>();
            List<DomainEventStream> streams = map.get(group);
            streams.forEach(steam -> {
                Object[] objects = new Object[] { group.getAggregateType(), group.getAggregateId(), steam.getVersion(), steam.getCommandId(), new Date(), JSONObject.toJSONString(steam.getEvents()) };
                batchParams.add(objects);
            });
            AggregateEventAppendResult appendResult = new AggregateEventAppendResult();
            appendResult.setGroup(group);
            try {
                jdbcTemplate.batchUpdate(INSERT_AGGREGATE_EVENTS, batchParams);
                appendResult.setEventAppendStatus(EventAppendStatus.Success);
                return appendResult;
            } catch (Throwable e) {
                if (e instanceof DuplicateKeyException) {
                    BatchUpdateException exception = (BatchUpdateException) e.getCause();
                    if (sqlState.equals(exception.getSQLState()) && exception.getMessage().contains(eventTableVersionUniqueIndexName)) {
                        appendResult.setEventAppendStatus(EventAppendStatus.DuplicateEvent);
                        appendResult.setThrowable(new AggregateEventConflictException(group.getAggregateId(), group.getAggregateType(), exception));
                        return appendResult;
                    } else if (sqlState.equals(exception.getSQLState()) && exception.getMessage().contains(eventTableCommandIdUniqueIndexName)) {
                        String commandId = getDuplicatedId(exception.getMessage());
                        if (StringUtils.isNotBlank(commandId)) {
                            appendResult.setEventAppendStatus(EventAppendStatus.DuplicateCommand);
                            appendResult.setDuplicateCommandIds(Lists.newArrayList(commandId));
                            appendResult.setThrowable(new AggregateCommandConflictException(group.getAggregateId(), group.getAggregateType(), Long.parseLong(commandId), exception));
                            return appendResult;
                        } else {
                            appendResult.setEventAppendStatus(EventAppendStatus.Exception);
                            appendResult.setThrowable(new EventStoreException("no found commandId cannot be resolved ", exception));
                            return appendResult;
                        }
                    }
                }
                appendResult.setEventAppendStatus(EventAppendStatus.Exception);
                appendResult.setThrowable(new EventStoreException(e));
                return appendResult;
            }
        }).collect(Collectors.toList());
        return CompletableFuture.completedFuture(resultList);
    }

    @Override
    public CompletableFuture<List<List<Event>>> load(long aggregateId, Class<? extends Aggregate> aggregateClass, int startVersion, int endVersion) {
        try {
            List<Map<String, Object>> rows = jdbcTemplate.queryForList(QUERY_AGGREGATE_EVENTS, new Object[] { aggregateId, startVersion, endVersion });
            List<List<Event>> events = rows.stream().map(map -> {
                String eventJson = (String) map.get("events");
                JSONArray array = JSONArray.parseArray(eventJson);
                List<Event> es = new ArrayList<>(array.size());
                array.forEach(object -> {
                    JSONObject jsonObject = (JSONObject) object;
                    String eventType = jsonObject.getString("eventType");
                    Event event = (Event) JSONObject.parseObject(jsonObject.toString(), ReflectUtils.getClass(eventType));
                    es.add(event);
                });
                return es;
            }).collect(Collectors.toList());
            return CompletableFuture.completedFuture(events);
        } catch (Throwable e) {
            CompletableFuture<List<List<Event>>> future = new CompletableFuture<>();
            future.completeExceptionally(e);
            return future;
        }

    }

    /**
     * Duplicate entry '5d3ac841d1fcfe669e9a257d-5d3ac841d1fcfe669e9a2585' for key 'IX_EventStream_AggId_CommandId'
     */
    @Override
    public String getDuplicatedId(String message) {
        Matcher matcher = PATTERN_MYSQL.matcher(message);
        if ((!matcher.find()) || (matcher.groupCount() == 0)) {
            return "";
        } else {
            return matcher.group(1);
        }
    }

}
