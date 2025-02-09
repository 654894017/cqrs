package com.damon.cqrs.event_store;

import com.alibaba.fastjson.JSONObject;
import com.damon.cqrs.event.AggregateEventAppendResult;
import com.damon.cqrs.event.DomainEventStream;
import com.damon.cqrs.exception.AggregateCommandConflictException;
import com.damon.cqrs.exception.AggregateEventConflictException;
import com.damon.cqrs.exception.EventStoreException;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.dbutils.QueryRunner;

import javax.sql.DataSource;
import java.sql.SQLException;
import java.util.*;
import java.util.function.Supplier;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

@Slf4j
public class EventStoreSupplier implements Supplier<AggregateEventAppendResult> {
    private static final Pattern PATTERN_MYSQL = Pattern.compile("^Duplicate entry '(.*)-(.*)' for key");
    private static final String INSERT_AGGREGATE_EVENTS = "INSERT INTO %s (aggregate_root_type_name, aggregate_root_id, version, command_id, gmt_create, events) VALUES (?, ?, ?, ?, ?, ?)";
    private static final String SQL_STATE_CONFLICT = "23000";
    private static final String EVENT_TABLE_VERSION_UNIQUE_INDEX_NAME = "uk_aggregate_id_version";
    private static final String EVENT_TABLE_COMMAND_ID_UNIQUE_INDEX_NAME = "uk_aggregate_id_command_id";

    private final DataSource dataSource;
    private final String tableName;
    private final List<DomainEventStream> eventStreams;

    public EventStoreSupplier(DataSource dataSource, String tableName, List<DomainEventStream> eventStreams) {
        this.dataSource = dataSource;
        this.tableName = tableName;
        this.eventStreams = eventStreams;
    }

    @Override
    public AggregateEventAppendResult get() {
        AggregateEventAppendResult result = new AggregateEventAppendResult();
        QueryRunner queryRunner = new QueryRunner(dataSource);
        Map<Long, String> aggregateTypeMap = new HashMap<>();
        List<Object[]> batchParams = new ArrayList<>();

        prepareBatchParams(aggregateTypeMap, batchParams);

        try {
            queryRunner.batch(String.format(INSERT_AGGREGATE_EVENTS, tableName), batchParams.toArray(new Object[0][]));
            addSuccessResults(result);
        } catch (SQLException exception) {
            handleSQLException(exception, result, aggregateTypeMap);
        } catch (Throwable exception) {
            log.error("Store event failed", exception);
            handleUnexpectedException(exception, result);
        }

        return result;
    }

    private void prepareBatchParams(Map<Long, String> aggregateTypeMap, List<Object[]> batchParams) {
        eventStreams.forEach(stream -> {
            batchParams.add(new Object[]{
                    stream.getAggregateType(), stream.getAggregateId(), stream.getVersion(), stream.getCommandId(), new Date(), JSONObject.toJSONString(stream.getEvents())
            });
            aggregateTypeMap.put(stream.getAggregateId(), stream.getAggregateType());
        });
    }

    private void addSuccessResults(AggregateEventAppendResult result) {
        eventStreams.forEach(stream -> {
            AggregateEventAppendResult.SucceedResult succeedResult = new AggregateEventAppendResult.SucceedResult();
            succeedResult.setCommandId(stream.getCommandId());
            succeedResult.setVersion(stream.getVersion());
            succeedResult.setAggregateType(stream.getAggregateType());
            succeedResult.setAggregateId(stream.getAggregateId());
            result.addSuccedResult(succeedResult);
        });
    }

    private void handleSQLException(SQLException exception, AggregateEventAppendResult result, Map<Long, String> aggregateTypeMap) {
        log.warn("Failed to store events. SQL state: {}, message: {}", exception.getSQLState(), exception.getMessage(), exception);

        if (SQL_STATE_CONFLICT.equals(exception.getSQLState())) {
            if (exception.getMessage().contains(EVENT_TABLE_VERSION_UNIQUE_INDEX_NAME)) {
                handleVersionConflict(exception, result, aggregateTypeMap);
            } else if (exception.getMessage().contains(EVENT_TABLE_COMMAND_ID_UNIQUE_INDEX_NAME)) {
                handleCommandIdConflict(exception, result, aggregateTypeMap);
            }
        } else {
            handleUnexpectedException(exception, result);
        }
    }

    private void handleVersionConflict(SQLException exception, AggregateEventAppendResult result, Map<Long, String> aggregateTypeMap) {
        String aggregateId = getExceptionId(exception.getMessage(), 1);
        String aggregateType = aggregateTypeMap.get(Long.parseLong(aggregateId));

        AggregateEventAppendResult.DuplicateEventResult duplicateEventResult = new AggregateEventAppendResult.DuplicateEventResult();
        duplicateEventResult.setAggreateId(Long.parseLong(aggregateId));
        duplicateEventResult.setAggregateType(aggregateType);
        duplicateEventResult.setThrowable(new AggregateEventConflictException(Long.parseLong(aggregateId), aggregateType, exception));
        result.addDuplicateEventResult(duplicateEventResult);
    }

    private void handleCommandIdConflict(SQLException exception, AggregateEventAppendResult result, Map<Long, String> aggregateTypeMap) {
        String commandId = getExceptionId(exception.getMessage(), 2);
        String aggregateId = getExceptionId(exception.getMessage(), 1);
        String aggregateType = aggregateTypeMap.get(Long.parseLong(aggregateId));

        AggregateEventAppendResult.DulicateCommandResult duplicateCommandResult = new AggregateEventAppendResult.DulicateCommandResult();
        duplicateCommandResult.setThrowable(new AggregateCommandConflictException(Long.parseLong(aggregateId), aggregateType, Long.parseLong(commandId), exception));
        duplicateCommandResult.setAggreateId(Long.parseLong(aggregateId));
        duplicateCommandResult.setCommandId(commandId);
        duplicateCommandResult.setAggregateType(aggregateType);
        result.addDulicateCommandResult(duplicateCommandResult);

        eventStreams.stream()
                .filter(stream -> !aggregateId.equals(stream.getAggregateId().toString()))
                .forEach(stream -> {
                    AggregateEventAppendResult.DulicateCommandResult normalCommandResult = new AggregateEventAppendResult.DulicateCommandResult();
                    normalCommandResult.setThrowable(new AggregateEventConflictException(Long.parseLong(aggregateId), aggregateType, exception));
                    normalCommandResult.setAggreateId(Long.parseLong(aggregateId));
                    normalCommandResult.setAggregateType(aggregateType);
                    result.addDulicateCommandResult(normalCommandResult);
                });
    }

    private void handleUnexpectedException(Throwable exception, AggregateEventAppendResult result) {
        eventStreams.forEach(stream -> {
            AggregateEventAppendResult.ExceptionResult exceptionResult = new AggregateEventAppendResult.ExceptionResult();
            exceptionResult.setThrowable(new EventStoreException("Event store exception", exception));
            exceptionResult.setAggreateId(stream.getAggregateId());
            exceptionResult.setAggregateType(stream.getAggregateType());
            result.addExceptionResult(exceptionResult);
        });
    }

    private String getExceptionId(String message, int index) {
        Matcher matcher = PATTERN_MYSQL.matcher(message);
        if (matcher.find() && matcher.groupCount() >= index) {
            return matcher.group(index);
        }
        return "";
    }
}