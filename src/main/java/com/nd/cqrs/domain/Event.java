package com.nd.cqrs.domain;

import java.time.ZonedDateTime;

import lombok.ToString;

@ToString
public abstract class Event {
    private String eventType;
    private long aggregateId;
    private String aggregateType;
    private ZonedDateTime timestamp;
    private int version;

    public Event() {
        this.eventType = this.getClass().getTypeName();
        this. timestamp = ZonedDateTime.now();
    }

    public long getAggregateId() {
        return aggregateId;
    }

    public ZonedDateTime getTimestamp() {
        return this.timestamp;
    }

    public int getVersion() {
        return version;
    }

    public Event setVersion(int version) {
        this.version = version;
        return this;
    }

    public Event setAggregateId(long aggregateId) {
        this.aggregateId = aggregateId;
        return this;
    }

    public Event setTimestamp(ZonedDateTime timestamp) {
        this.timestamp = timestamp;
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
