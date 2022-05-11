package com.damon.cqrs.domain;

import lombok.ToString;

import java.io.Serializable;
import java.time.ZonedDateTime;

/**
 * 注意：如果子类实现新的构造方法，子类一定要实现一个无参构造方法，否则事件在JSON序列化时会导致丢失Event的数据。
 *
 * @author xianping_lu
 */
@ToString
public abstract class Event implements Serializable {
    /**
     *
     */
    private static final long serialVersionUID = -879757997924919709L;
    private String eventType;
    private long aggregateId;
    private String aggregateType;
    private ZonedDateTime timestamp;
    private int version;

    public Event() {
        this.eventType = this.getClass().getTypeName();
        this.timestamp = ZonedDateTime.now();
    }

    public long getAggregateId() {
        return aggregateId;
    }

    public Event setAggregateId(long aggregateId) {
        this.aggregateId = aggregateId;
        return this;
    }

    public ZonedDateTime getTimestamp() {
        return this.timestamp;
    }

    public Event setTimestamp(ZonedDateTime timestamp) {
        this.timestamp = timestamp;
        return this;
    }

    public int getVersion() {
        return version;
    }

    public Event setVersion(int version) {
        this.version = version;
        return this;
    }

    public String getAggregateType() {
        return aggregateType;
    }

    public Event setAggregateType(String aggregateType) {
        this.aggregateType = aggregateType;
        return this;
    }

    public String getEventType() {
        return eventType;
    }

    public void setEventType(String eventType) {
        this.eventType = eventType;
    }

}
