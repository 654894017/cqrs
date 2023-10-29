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
    private final static Pattern PATTERN_MYSQL = Pattern.compile("^Duplicate entry '(.*)-(.*)' for key");
    private final DataSource dataSource;
    private final String tableName;
    private final ArrayList<DomainEventStream> eventStreams;
    private final String INSERT_AGGREGATE_EVENTS = "INSERT INTO %s ( aggregate_root_type_name ,  aggregate_root_id ,  version ,  command_id ,  gmt_create ,  events ) VALUES (?, ?, ?, ?, ?, ?)";
    private final String sqlState = "23000";
    private final String eventTableVersionUniqueIndexName = "uk_aggregate_id_version";
    private final String eventTableCommandIdUniqueIndexName = "uk_aggregate_id_command_id";

    public EventStoreSupplier(DataSource dataSource, String tableName, ArrayList<DomainEventStream> eventStreams) {
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
        } catch (SQLException exception) {
            log.warn("store event failed ", exception);
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
            } else {
                processUnableException(exception, result);
            }
        } catch (Throwable exception) {
            log.error("store event failed ", exception);
            processUnableException(exception, result);
        }
        return result;
    }

    /**
     * 处理未被捕获的异常统一都转换为 EventStoreException 异常
     *
     * @param exception
     * @param result
     */
    private void processUnableException(Throwable exception, AggregateEventAppendResult result) {
        AggregateEventAppendResult.ExceptionResult exceptionResult = new AggregateEventAppendResult.ExceptionResult();
        Map<Long, Boolean> flag = new HashMap<>();
        eventStreams.forEach(stream -> {
            if (flag.get(stream.getAggregateId())) {
                exceptionResult.setThrowable(new EventStoreException("event store exception ", exception));
                exceptionResult.setAggreateId(stream.getAggregateId());
                exceptionResult.setAggregateType(stream.getAggregateType());
                result.addExceptionResult(exceptionResult);
                flag.put(stream.getAggregateId(), Boolean.TRUE);
            }
        });
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
