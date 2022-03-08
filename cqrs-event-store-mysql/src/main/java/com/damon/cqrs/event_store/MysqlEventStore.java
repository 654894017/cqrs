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
import com.damon.cqrs.utils.NamedThreadFactory;
import com.damon.cqrs.utils.ReflectUtils;
import com.google.common.collect.ImmutableMap;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.jdbc.core.JdbcTemplate;

import javax.sql.DataSource;
import java.sql.BatchUpdateException;
import java.util.*;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
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
    private final String EVENT_TABLE = "event_stream_";
    private final String QUERY_AGGREGATE_EVENTS = "SELECT events FROM %s WHERE aggregate_root_id = ?  and  version >= ? and version <= ? ORDER BY version asc";
    private final String INSERT_AGGREGATE_EVENTS = "INSERT INTO %s ( aggregate_root_type_name ,  aggregate_root_id ,  version ,  command_id ,  gmt_create ,  events ) VALUES (?, ?, ?, ?, ?, ?)";
    private final String QUERY_AGGREGATE_WAITING_SEND_EVENTS = "SELECT * FROM %s  WHERE ID > ? ORDER BY  id ASC  LIMIT 20000";
    private final String sqlState = "23000";
    private final String eventTableVersionUniqueIndexName = "uk_aggregate_id_version";
    private final String eventTableCommandIdUniqueIndexName = "uk_aggregate_id_command_id";
    private final JdbcTemplate jdbcTemplate = new JdbcTemplate();
    private Map<DataSource, Integer> dataSourceMap;
    private Map<String, DataSource> dataSourceNameMap;
    private ExecutorService eventStoreThreadService;

    /**
     * @param dataSourceMappings
     * @param storeThreadNumber  事件存储异步线程处理数。如果存在分库、分表数量较多，需要调整此大小。
     */
    public MysqlEventStore(final List<DataSourceMapping> dataSourceMappings, final int storeThreadNumber) {
        dataSourceMap = new HashMap<>();
        dataSourceNameMap = new HashMap<>();
        dataSourceMappings.forEach(mapping -> {
            dataSourceMap.put(mapping.getDataSource(), mapping.getTableNumber());
            dataSourceNameMap.put(mapping.getDataSourceName(), mapping.getDataSource());
        });
        this.eventStoreThreadService = Executors.newFixedThreadPool(storeThreadNumber, new NamedThreadFactory("event-store-pool"));
    }

    @Override
    public CompletableFuture<List<EventSendingContext>> queryWaitingSendEvents(String dataSourceName, String tableName, long offsetId) {
        try {
            jdbcTemplate.setDataSource(dataSourceNameMap.get(dataSourceName));
            List<Map<String, Object>> rows = jdbcTemplate.queryForList(String.format(QUERY_AGGREGATE_WAITING_SEND_EVENTS, tableName), offsetId);
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

        Map<Long, List<DomainEventStream>> map = domainEventStreams.stream().collect(
                Collectors.groupingBy(DomainEventStream::getAggregateId)
        );
        Set<Long> aggregateIds = map.keySet();
        //构建event对应的数据源与数据表，批量插入使用
        Map<DataSource, Map<String, List<DomainEventStream>>> dataSourceListMap = new HashMap<>();
        aggregateIds.forEach(aggregateId -> {
            DataSource dataSource = DataRoute.routeDataSource(aggregateId, dataSourceMap.keySet().stream().collect(Collectors.toList()));
            Integer tableNumber = dataSourceMap.get(dataSource);
            Integer tableIndex = DataRoute.routeTable(aggregateId, tableNumber);
            String tableName = EVENT_TABLE + tableIndex;
            Map<String, List<DomainEventStream>> listMap = dataSourceListMap.get(dataSource);
            if (listMap == null) {
                dataSourceListMap.put(dataSource, ImmutableMap.of(tableName, map.get(aggregateId)));
            } else {
                List<DomainEventStream> eventStreams = listMap.get(tableName);
                eventStreams.addAll(map.get(aggregateId));
            }
        });

        AggregateEventAppendResult result = new AggregateEventAppendResult();
        Map<Long, String> aggregateTypeMap = new HashMap<>();
        dataSourceListMap.forEach((dataSource, tableEventStreamMap) -> {
            tableEventStreamMap.forEach((tableName, eventStreams) -> {
                CompletableFuture.runAsync(() -> {
                    jdbcTemplate.setDataSource(dataSource);
                    List<Object[]> batchParams = new ArrayList<>();
                    eventStreams.forEach(stream -> {
                        Object[] objects = new Object[]{
                                stream.getAggregateType(), stream.getAggregateId(), stream.getVersion(), stream.getCommandId(), new Date(), JSONObject.toJSONString(stream.getEvents())
                        };
                        aggregateTypeMap.put(stream.getAggregateId(), stream.getAggregateType());
                        batchParams.add(objects);
                    });
                    try {
                        jdbcTemplate.batchUpdate(String.format(INSERT_AGGREGATE_EVENTS, tableName), batchParams);
                        eventStreams.forEach(stream -> {
                            AggregateEventAppendResult.SucceedResult succeedResult = new AggregateEventAppendResult.SucceedResult();
                            succeedResult.setFuture(stream.getFuture());
                            succeedResult.setCommandId(stream.getCommandId());
                            succeedResult.setVersion(stream.getVersion());
                            succeedResult.setAggregateType(stream.getAggregateType());
                            succeedResult.setAggregateId(stream.getAggregateId());
                            result.addSuccedResult(succeedResult);
                        });
                    } catch (Throwable e) {
                        if (e instanceof DuplicateKeyException) {
                            BatchUpdateException exception = (BatchUpdateException) e.getCause();
                            if (sqlState.equals(exception.getSQLState()) && exception.getMessage().contains(eventTableVersionUniqueIndexName)) {
                                String aggregateId = getExceptionId(exception.getMessage(), 1);
                                String aggreagetType = aggregateTypeMap.get(Long.parseLong(aggregateId));
                                Map<Long, Boolean> flag = new HashMap<>();
                                eventStreams.forEach(stream -> {
                                    if (flag.get(stream.getAggregateId()) == null) {
                                        AggregateEventAppendResult.DuplicateEventResult duplicateEventResult = new AggregateEventAppendResult.DuplicateEventResult();
                                        duplicateEventResult.setAggreateId(Long.parseLong(aggregateId));
                                        duplicateEventResult.setAggregateType(aggreagetType);
                                        duplicateEventResult.setThrowable(new AggregateEventConflictException(Long.parseLong(aggregateId), aggreagetType, exception));
                                        result.addDuplicateEventResult(duplicateEventResult);
                                        flag.put(stream.getAggregateId(), Boolean.TRUE);
                                    }
                                });
                            } else if (sqlState.equals(exception.getSQLState()) && exception.getMessage().contains(eventTableCommandIdUniqueIndexName)) {
                                String commandId = getExceptionId(exception.getMessage(), 2);
                                String aggregateId = getExceptionId(exception.getMessage(), 1);
                                String aggreagetType = aggregateTypeMap.get(Long.parseLong(aggregateId));
                                AggregateEventAppendResult.DulicateCommandResult dulicateCommandResult = new AggregateEventAppendResult.DulicateCommandResult();
                                dulicateCommandResult.setThrowable(new AggregateCommandConflictException(Long.parseLong(aggregateId), aggreagetType, Long.parseLong(commandId), exception));
                                dulicateCommandResult.setAggreateId(Long.parseLong(aggregateId));
                                dulicateCommandResult.setCommandId(commandId);
                                dulicateCommandResult.setAggregateType(aggreagetType);
                                result.addDulicateCommandResult(dulicateCommandResult);
                                Map<Long, Boolean> flag = new HashMap<>();
                                eventStreams.forEach(stream -> {
                                    if (flag.get(stream.getAggregateId()) == null && !aggregateId.equals(stream.getAggregateId().toString())) {
                                        AggregateEventAppendResult.DulicateCommandResult normalCommandResult = new AggregateEventAppendResult.DulicateCommandResult();
                                        /**
                                         * commandId冲突标识当前commandId的异常为AggregateCommandConflictException，
                                         * 其余的commandId异常标识为AggregateEventConflictException，交由业务方重试
                                         */
                                        normalCommandResult.setThrowable(new AggregateEventConflictException(Long.parseLong(aggregateId), aggreagetType, exception));
                                        normalCommandResult.setAggreateId(Long.parseLong(aggregateId));
                                        normalCommandResult.setAggregateType(aggreagetType);
                                        result.addDulicateCommandResult(normalCommandResult);
                                        flag.put(stream.getAggregateId(), Boolean.TRUE);
                                    }
                                });
                            }
                        } else {
                            AggregateEventAppendResult.ExceptionResult exceptionResult = new AggregateEventAppendResult.ExceptionResult();
                            Map<Long, Boolean> flag = new HashMap<>();
                            eventStreams.forEach(stream -> {
                                if (flag.get(stream.getAggregateId())) {
                                    exceptionResult.setThrowable(new EventStoreException("event store exception ", e));
                                    exceptionResult.setAggreateId(stream.getAggregateId());
                                    exceptionResult.setAggregateType(stream.getAggregateType());
                                    flag.put(stream.getAggregateId(), Boolean.TRUE);
                                }
                            });
                        }
                    }
                }, eventStoreThreadService).join();
            });
        });
        return CompletableFuture.completedFuture(result);
    }

    @Override
    public CompletableFuture<List<List<Event>>> load(long aggregateId, Class<? extends Aggregate> aggregateClass, int startVersion, int endVersion) {
        try {
            DataSource dataSource = DataRoute.routeDataSource(aggregateId, dataSourceMap.keySet().stream().collect(Collectors.toList()));
            jdbcTemplate.setDataSource(dataSource);
            Integer tableNumber = dataSourceMap.get(dataSource);
            Integer tableIndex = DataRoute.routeTable(aggregateId, tableNumber);
            String tableName = EVENT_TABLE + tableIndex;
            List<Map<String, Object>> rows = jdbcTemplate.queryForList(String.format(QUERY_AGGREGATE_EVENTS, tableName), aggregateId, startVersion, endVersion);
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
