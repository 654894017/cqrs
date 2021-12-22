package com.damon.cqrs.domain;

import java.io.Serializable;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.time.ZonedDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Queue;
import java.util.concurrent.ConcurrentLinkedQueue;

import com.google.common.base.Preconditions;
import com.google.common.base.Throwables;
import com.google.common.collect.Lists;

/**
 * 
 * 聚合根抽象
 * 
 *  注意：子类必须生成所有属性的get set方法，用于生成聚合快照复制时用。
 * 
 * @author xianping_lu
 *
 */
public abstract class Aggregate implements Serializable {

    /**
     * 
     */
    private static final long serialVersionUID = 1750836984371267776L;
    private long id;
    private int version;
    private List<Event> emptyEvents = new ArrayList<>();
    private Queue<Event> uncommittedEvents = new ConcurrentLinkedQueue<>();
    private ZonedDateTime timestamp;
    private ZonedDateTime lastSnapshootTimestamp = ZonedDateTime.now();

    public Aggregate() {
        Preconditions.checkNotNull(id);
    }

    public Aggregate(long id) {
        Preconditions.checkNotNull(id);
        this.id = id;
    }

    public int getVersion() {
        return version;
    }

    public ZonedDateTime getTimestamp() {
        return timestamp;
    }

    public void setTimestamp(ZonedDateTime timestamp) {
        this.timestamp = timestamp;
    }

    public void setVersion(int baseVersion) {
        this.version = baseVersion;
    }

    public ZonedDateTime getLastSnapshootTimestamp() {
        return lastSnapshootTimestamp;
    }

    public void setLastSnapshootTimestamp(ZonedDateTime lastSnapshootTimestamp) {
        this.lastSnapshootTimestamp = lastSnapshootTimestamp;
    }

    private void appendUncommittedEvent(Event event) {
        if (uncommittedEvents == null) {
            uncommittedEvents = new ConcurrentLinkedQueue<>();
        }
        if (uncommittedEvents.stream().anyMatch(x -> x.getClass().equals(event.getClass()))) {
            throw new UnsupportedOperationException(
                String.format("Cannot apply duplicated domain event type: %s, current aggregateRoot type: %s, id: %s",
                    event.getClass(), this.getClass().getName(), id));
        }
        uncommittedEvents.add(event);
    }

    /**
     * 聚合根事件触发
     * 
     * @param event
     */
    protected void applyNewEvent(Event event) {
        apply(event);
        event.setVersion(version + 1);
        event.setAggregateId(getId());
        event.setAggregateType(this.getClass().getTypeName());
        appendUncommittedEvent(event);
        this.timestamp = ZonedDateTime.now();
    }

    @SuppressWarnings("deprecation")
    private void apply(Event event) {
        try {
            Method method = this.getClass().getDeclaredMethod("apply", event.getClass());
            method.setAccessible(true);
            method.invoke(this, event);
        } catch (InvocationTargetException e) {
            Throwables.propagate(e.getCause());
        } catch (NoSuchMethodException | IllegalAccessException e) {
            throw new UnsupportedOperationException(
                String.format("Aggregate '%s' doesn't apply event type '%s'", this.getClass(), event.getClass()), e);
        }
    }

    /**
     * 获取聚合根ID
     * 
     * @return
     */
    public long getId() {
        return id;
    }

    public void setId(long id) {
        this.id = id;
    }

    /**
     * 获取未提交的事件
     * 
     * @return
     */
    public List<Event> getChanges() {
        if (uncommittedEvents == null) {
            return emptyEvents;
        }
        return Lists.newArrayList(uncommittedEvents);
    }

    /**
     * 接受聚合根变更
     * 
     */
    public void acceptChanges() {
        if (uncommittedEvents != null && !uncommittedEvents.isEmpty()) {
            version = uncommittedEvents.peek().getVersion();
            uncommittedEvents.clear();
        }
    }

    /**
     * 验证聚合根事件是否有效
     * 
     * @param event
     */
    private void verifyEvent(Event event) {
        if (event.getVersion() > 1 && event.getAggregateId() != this.getId()) {
            throw new UnsupportedOperationException(
                String.format("Invalid domain event stream, aggregateRootId:%s, expected aggregateRootId:%s, type:%s",
                    event.getAggregateId(), this.getId(), this.getClass().getName()));
        }
        if (event.getVersion() != this.version + 1) {
            throw new UnsupportedOperationException(String.format(
                "Invalid domain event stream, version: %d, expected version: %d, current aggregateRoot type: %s, id: %s",
                event.getVersion(), this.version, this.getClass().getName(), this.getId()));
        }
    }

    /**
     * 聚合事件回放
     * 
     * @param events
     */
    public void replayEvents(List<Event> events) {
        if (events == null || events.size() == 0) {
            return;
        }
        events.forEach(event -> {
            verifyEvent(event);
            apply(event);
            this.version = event.getVersion();
        });
        ZonedDateTime now = ZonedDateTime.now();
        this.timestamp = now;
        this.lastSnapshootTimestamp = now;
    }

    /**
     * 创建聚合根快照周期(单位秒，小于0不创建).
     * @return
     */
    public abstract long createSnapshootCycle();

}
