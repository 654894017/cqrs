package com.damon.cqrs.event_store;

import com.alibaba.fastjson.JSONArray;
import com.alibaba.fastjson.JSONObject;
import com.damon.cqrs.domain.AggregateRoot;
import com.damon.cqrs.domain.Event;
import com.damon.cqrs.event.AggregateEventAppendResult;
import com.damon.cqrs.event.DomainEventStream;
import com.damon.cqrs.event.EventSendingContext;
import com.damon.cqrs.exception.AggregateCommandConflictException;
import com.damon.cqrs.exception.AggregateEventConflictException;
import com.damon.cqrs.exception.EventStoreException;
import com.damon.cqrs.store.IEventShardingRouting;
import com.damon.cqrs.store.IEventStore;
import com.damon.cqrs.utils.NamedThreadFactory;
import com.damon.cqrs.utils.ReflectUtils;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.dbutils.QueryRunner;
import org.apache.commons.dbutils.handlers.MapListHandler;

import javax.sql.DataSource;
import java.sql.SQLException;
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
@Slf4j
public class MysqlEventStore implements IEventStore {

    private final static Pattern PATTERN_MYSQL = Pattern.compile("^Duplicate entry '(.*)-(.*)' for key");
    private final String EVENT_TABLE = "event_stream_";
    private final String QUERY_AGGREGATE_EVENTS = "SELECT events FROM %s WHERE aggregate_root_id = ?  and  version >= ? and version <= ? ORDER BY version asc";
    private final String INSERT_AGGREGATE_EVENTS = "INSERT INTO %s ( aggregate_root_type_name ,  aggregate_root_id ,  version ,  command_id ,  gmt_create ,  events ) VALUES (?, ?, ?, ?, ?, ?)";
    private final String QUERY_AGGREGATE_WAITING_SEND_EVENTS = "SELECT * FROM %s  WHERE ID > ? ORDER BY  id ASC  LIMIT 20000";
    private final String sqlState = "23000";
    private final String eventTableVersionUniqueIndexName = "uk_aggregate_id_version";
    private final String eventTableCommandIdUniqueIndexName = "uk_aggregate_id_command_id";
    private final Map<DataSource, Integer> dataSourceMap;
    private final Map<String, DataSource> dataSourceNameMap;
    private final ExecutorService eventStoreThreadService;
    private final List<DataSource> dataSources;
    private final IEventShardingRouting eventShardingRoute;

    /**
     * @param dataSourceMappings
     * @param storeThreadNumber  事件存储异步线程处理数。如果存在分库、分表数量较多，需要调整此大小。
     */
    public MysqlEventStore(final List<DataSourceMapping> dataSourceMappings, final int storeThreadNumber, final IEventShardingRouting eventShardingRoute) {
        this.dataSourceMap = new HashMap<>();
        this.dataSourceNameMap = new HashMap<>();
        this.dataSources = new ArrayList<>();
        dataSourceMappings.forEach(mapping -> {
            dataSourceMap.put(mapping.getDataSource(), mapping.getTableNumber());
            dataSourceNameMap.put(mapping.getDataSourceName(), mapping.getDataSource());
            dataSources.add(mapping.getDataSource());
        });
        this.eventStoreThreadService = Executors.newFixedThreadPool(storeThreadNumber, new NamedThreadFactory("event-store-pool"));
        this.eventShardingRoute = eventShardingRoute;
    }

    @Override
    public CompletableFuture<List<EventSendingContext>> queryWaitingSendEvents(String dataSourceName, String tableName, long offsetId) {
        try {
            QueryRunner queryRunner = new QueryRunner(dataSourceNameMap.get(dataSourceName));
            List<Map<String, Object>> rows = queryRunner.query(String.format(QUERY_AGGREGATE_WAITING_SEND_EVENTS, tableName), new MapListHandler(), offsetId);
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
        //事件分片
        HashMap<DataSource, HashMap<String, ArrayList<DomainEventStream>>> dataSourceListMap = eventSharding(domainEventStreams);
        AggregateEventAppendResult result = new AggregateEventAppendResult();
        Map<Long, String> aggregateTypeMap = new HashMap<>();
        dataSourceListMap.forEach((dataSource, tableEventStreamMap) -> {
            tableEventStreamMap.forEach((tableName, eventStreams) -> {
                CompletableFuture.runAsync(() -> {
                    QueryRunner queryRunner = new QueryRunner(dataSource);
                    List<Object[]> batchParams = new ArrayList<>();
                    eventStreams.forEach(stream -> {
                        batchParams.add(new Object[]{
                                stream.getAggregateType(), stream.getAggregateId(), stream.getVersion(), stream.getCommandId(), new Date(), JSONObject.toJSONString(stream.getEvents())
                        });
                        aggregateTypeMap.put(stream.getAggregateId(), stream.getAggregateType());
                    });
                    try {
                        queryRunner.batch(String.format(INSERT_AGGREGATE_EVENTS, tableName), batchParams.toArray(new Object[batchParams.size()][]));
                        eventStreams.forEach(stream -> {
                            AggregateEventAppendResult.SucceedResult succeedResult = new AggregateEventAppendResult.SucceedResult();
                            succeedResult.setCommandId(stream.getCommandId());
                            succeedResult.setVersion(stream.getVersion());
                            succeedResult.setAggregateType(stream.getAggregateType());
                            succeedResult.setAggregateId(stream.getAggregateId());
                            result.addSuccedResult(succeedResult);
                        });
                    } catch (Throwable e) {
                        log.warn("store event failed ", e);
                        if (e instanceof SQLException) {
                            SQLException exception = (SQLException) e;
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
                                return;
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
                                return;
                            }
                        }
                        AggregateEventAppendResult.ExceptionResult exceptionResult = new AggregateEventAppendResult.ExceptionResult();
                        Map<Long, Boolean> flag = new HashMap<>();
                        eventStreams.forEach(stream -> {
                            if (flag.get(stream.getAggregateId())) {
                                exceptionResult.setThrowable(new EventStoreException("event store exception ", e));
                                exceptionResult.setAggreateId(stream.getAggregateId());
                                exceptionResult.setAggregateType(stream.getAggregateType());
                                result.addExceptionResult(exceptionResult);
                                flag.put(stream.getAggregateId(), Boolean.TRUE);
                            }
                        });
                    }
                }, eventStoreThreadService).join();
            });
        });
        return CompletableFuture.completedFuture(result);
    }

    private HashMap<DataSource, HashMap<String, ArrayList<DomainEventStream>>> eventSharding(List<DomainEventStream> domainEventStreams) {
        HashMap<DataSource, HashMap<String, ArrayList<DomainEventStream>>> dataSourceListMap = new HashMap<>();
        domainEventStreams.forEach(event -> {
            Integer dataSourceIndex = eventShardingRoute.routeInstance(event.getAggregateId(), event.getAggregateType(), dataSources.size(), event.getShardingParams());
            Integer tableNumber = dataSourceMap.get(dataSources.get(dataSourceIndex));
            Integer tableIndex = eventShardingRoute.routeSharding(event.getAggregateId(), event.getAggregateType(), tableNumber, event.getShardingParams());
            String tableName = EVENT_TABLE + tableIndex;
            dataSourceListMap.computeIfAbsent(dataSources.get(dataSourceIndex), key -> new HashMap<>()).computeIfAbsent(tableName, key -> new ArrayList<>()).add(event);
        });
        return dataSourceListMap;
    }

    @Override
    public CompletableFuture<List<List<Event>>> load(long aggregateId, Class<? extends AggregateRoot> aggregateClass, int startVersion, int endVersion, Map<String, Object> shardingParams) {
        try {
            Integer dataSourceIndex = eventShardingRoute.routeInstance(aggregateId, aggregateClass.getTypeName(), dataSources.size(), shardingParams);
            QueryRunner queryRunner = new QueryRunner(dataSources.get(dataSourceIndex));
            Integer tableNumber = dataSourceMap.get(dataSources.get(dataSourceIndex));
            Integer tableIndex = eventShardingRoute.routeSharding(aggregateId, aggregateClass.getTypeName(), tableNumber, shardingParams);
            String tableName = EVENT_TABLE + tableIndex;
            List<Map<String, Object>> rows = queryRunner.query(String.format(QUERY_AGGREGATE_EVENTS, tableName), new MapListHandler(), aggregateId, startVersion, endVersion);
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
     * Duplicate entry '1486578438935470082-1486578443905720323' for key 'uk_aggregate_id_command_id'
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
