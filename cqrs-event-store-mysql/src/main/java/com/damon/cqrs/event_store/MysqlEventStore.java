package com.damon.cqrs.event_store;

import com.alibaba.fastjson.JSONArray;
import com.alibaba.fastjson.JSONObject;
import com.damon.cqrs.domain.Aggregate;
import com.damon.cqrs.domain.Event;
import com.damon.cqrs.event.AggregateEventAppendResult;
import com.damon.cqrs.event.DomainEventStream;
import com.damon.cqrs.event.EventSendingContext;
import com.damon.cqrs.exception.AggregateCommandConflictException;
import com.damon.cqrs.exception.AggregateEventConflictException;
import com.damon.cqrs.exception.EventStoreException;
import com.damon.cqrs.store.IEventStore;
import com.damon.cqrs.utils.ReflectUtils;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.jdbc.core.JdbcTemplate;

import javax.sql.DataSource;
import java.sql.BatchUpdateException;
import java.util.*;
import java.util.concurrent.CompletableFuture;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

/**
 * mysql事件存储器
 *
 * @author xianpinglu
 */
public class MysqlEventStore implements IEventStore {

    private final static Pattern PATTERN_MYSQL = Pattern.compile("^Duplicate entry '(.*)-(.*)' for key");
    private final String QUERY_AGGREGATE_EVENTS = "SELECT events FROM event_stream WHERE aggregate_root_id = ?  and  version >= ? and version <= ? ORDER BY version asc";
    private final String INSERT_AGGREGATE_EVENTS = "INSERT INTO event_stream ( aggregate_root_type_name ,  aggregate_root_id ,  version ,  command_id ,  gmt_create ,  events ) VALUES (?, ?, ?, ?, ?, ?)";
    private final String QUERY_AGGREGATE_WAITING_SEND_EVENTS = "SELECT * FROM event_stream  WHERE ID > ? ORDER BY  id ASC  LIMIT 20000";
    private final String sqlState = "23000";
    private final String eventTableVersionUniqueIndexName = "uk_aggregate_id_version";
    private final String eventTableCommandIdUniqueIndexName = "uk_aggregate_id_command_id";
    private final JdbcTemplate jdbcTemplate;

    public MysqlEventStore(final DataSource dataSource) {
        this.jdbcTemplate = new JdbcTemplate(dataSource);
    }

    @Override
    public CompletableFuture<List<EventSendingContext>> queryWaitingSendEvents(long offsetId) {
        try {
            List<Map<String, Object>> rows = jdbcTemplate.queryForList(QUERY_AGGREGATE_WAITING_SEND_EVENTS, offsetId);
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
                    Event event = JSONObject.parseObject(jsonObject.toString(), ReflectUtils.getClass(eventType));
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
    public CompletableFuture<AggregateEventAppendResult> store(List<DomainEventStream> domainEventStreams) {
        //System.out.println(domainEventStreams.size());
        List<Object[]> batchParams = new ArrayList<>();
        domainEventStreams.forEach(stream -> {
            Object[] objects = new Object[]{
                    stream.getAggregateType(),
                    stream.getAggregateId(),
                    stream.getVersion(),
                    stream.getCommandId(),
                    new Date(),
                    JSONObject.toJSONString(stream.getEvents())
            };
            batchParams.add(objects);
        });
        Map<Long,String> aggregateTypeMap = new HashMap<>();
        try {
            List<AggregateEventAppendResult> resultList = new ArrayList<>();
            jdbcTemplate.batchUpdate(INSERT_AGGREGATE_EVENTS, batchParams);

            AggregateEventAppendResult result = new AggregateEventAppendResult();
            domainEventStreams.forEach(stream -> {
                AggregateEventAppendResult.SucceedResult succeedResult = new AggregateEventAppendResult.SucceedResult();
                succeedResult.setFuture(stream.getFuture());
                succeedResult.setCommandId(stream.getCommandId());
                succeedResult.setVersion(stream.getVersion());
                succeedResult.setAggregateType(stream.getAggregateType());
                succeedResult.setAggregateId(stream.getAggregateId());
                aggregateTypeMap.put(stream.getAggregateId(),stream.getAggregateType());
                result.addSuccedResult(succeedResult);
            });
            return CompletableFuture.completedFuture(result);
        } catch (Throwable e) {
            AggregateEventAppendResult result = new AggregateEventAppendResult();
            if (e instanceof DuplicateKeyException) {
                BatchUpdateException exception = (BatchUpdateException) e.getCause();
                if (sqlState.equals(exception.getSQLState()) && exception.getMessage().contains(eventTableVersionUniqueIndexName)) {
                    String aggregateId = getExceptionId(exception.getMessage(), 1);
                    String aggreagetType = aggregateTypeMap.get(Long.parseLong(aggregateId));
                    AggregateEventAppendResult.DulicateResult dulicateResult = new AggregateEventAppendResult.DulicateResult();
                    dulicateResult.setAggreateId(Long.parseLong(aggregateId));
                    dulicateResult.setAggregateType(aggreagetType);
                    dulicateResult.setThrowable(new AggregateEventConflictException(Long.parseLong(aggregateId), aggreagetType, exception));
                    result.addDulicateResult(dulicateResult);

                    Map<Long, Boolean> flag = new HashMap<>();
                    domainEventStreams.forEach(stream -> {
                        if (flag.get(stream.getAggregateId()) == null && !aggregateId.equals(stream.getAggregateId().toString())) {
                            AggregateEventAppendResult.ExceptionResult exceptionResult = new AggregateEventAppendResult.ExceptionResult();
                            exceptionResult.setAggreateId(stream.getAggregateId());
                            exceptionResult.setAggregateType(stream.getAggregateType());
                            exceptionResult.setThrowable(new EventStoreException("event store exception ", e));
                            result.addExceptionResult(exceptionResult);
                            flag.put(stream.getAggregateId(), Boolean.TRUE);
                        }
                    });
                } else if (sqlState.equals(exception.getSQLState()) && exception.getMessage().contains(eventTableCommandIdUniqueIndexName)) {
                    String commandId = getExceptionId(exception.getMessage(), 2);
                    String aggregateId = getExceptionId(exception.getMessage(), 1);
                    String aggreagetType = aggregateTypeMap.get(Long.parseLong(aggregateId));
                    AggregateEventAppendResult.DulicateResult dulicateCommandResult = new AggregateEventAppendResult.DulicateResult();
                    dulicateCommandResult.setThrowable(new AggregateCommandConflictException(Long.parseLong(aggregateId), aggreagetType, Long.parseLong(commandId), exception));
                    dulicateCommandResult.setAggreateId(Long.parseLong(aggregateId));
                    dulicateCommandResult.setAggregateType(aggreagetType);
                    result.addDulicateResult(dulicateCommandResult);

                    Map<Long, Boolean> flag = new HashMap<>();
                    domainEventStreams.forEach(stream -> {
                        if (flag.get(stream.getAggregateId()) == null && !aggregateId.equals(stream.getAggregateId().toString())) {
                            AggregateEventAppendResult.ExceptionResult exceptionResult = new AggregateEventAppendResult.ExceptionResult();
                            exceptionResult.setAggreateId(stream.getAggregateId());
                            exceptionResult.setAggregateType(stream.getAggregateType());
                            exceptionResult.setThrowable(new EventStoreException("event store exception ", e));
                            result.addExceptionResult(exceptionResult);
                            flag.put(stream.getAggregateId(), Boolean.TRUE);
                        }
                    });
                }
            } else {
                Map<Long, Boolean> flag = new HashMap<>();
                domainEventStreams.forEach(stream -> {
                    if (flag.get(stream.getAggregateId()) == null) {
                        AggregateEventAppendResult.ExceptionResult exceptionResult = new AggregateEventAppendResult.ExceptionResult();
                        exceptionResult.setAggreateId(stream.getAggregateId());
                        exceptionResult.setAggregateType(stream.getAggregateType());
                        exceptionResult.setThrowable(new EventStoreException("no found commandId cannot be resolved ", e));
                        result.addExceptionResult(exceptionResult);
                        flag.put(stream.getAggregateId(), Boolean.TRUE);
                    }
                });
            }
            return CompletableFuture.completedFuture(result);
        }
    }

    @Override
    public CompletableFuture<List<List<Event>>> load(long aggregateId, Class<? extends Aggregate> aggregateClass, int startVersion, int endVersion) {
        try {
            List<Map<String, Object>> rows = jdbcTemplate.queryForList(QUERY_AGGREGATE_EVENTS, aggregateId, startVersion, endVersion);
            List<List<Event>> events = rows.stream().map(map -> {
                String eventJson = (String) map.get("events");
                JSONArray array = JSONArray.parseArray(eventJson);
                List<Event> es = new ArrayList<>(array.size());
                array.forEach(object -> {
                    JSONObject jsonObject = (JSONObject) object;
                    String eventType = jsonObject.getString("eventType");
                    Event event = JSONObject.parseObject(jsonObject.toString(), ReflectUtils.getClass(eventType));
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
     * Duplicate entry '1486578438935470082-1486578443905720323' for key 'uk_aggregate_root_id_command_id'
     */
    public String getExceptionId(String message, int index) {
        Matcher matcher = PATTERN_MYSQL.matcher(message);
        if ((!matcher.find()) || (matcher.groupCount() == 0)) {
            return "";
        } else {
            return matcher.group(index);
        }
    }

}
