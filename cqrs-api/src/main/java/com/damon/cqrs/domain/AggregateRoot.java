package com.damon.cqrs.domain;

import com.google.common.base.Throwables;
import com.google.common.collect.Lists;

import java.io.Serializable;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.time.ZonedDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Queue;
import java.util.concurrent.ConcurrentLinkedQueue;

/**
 * 聚合根抽象
 * <p>
 * 注意：子类必须生成所有属性的get set方法，用于生成聚合快照复制时用。聚合根需要实现空构造方法。
 *
 * @author xianping_lu
 */
public abstract class AggregateRoot implements Serializable {

    /**
     *
     */
    private static final long serialVersionUID = 1750836984371267776L;
    private final List<Event> emptyEvents = new ArrayList<>();
    //private Long id;
    private int version;
    private Queue<Event> uncommittedEvents = new ConcurrentLinkedQueue<>();
    private ZonedDateTime timestamp;
    private ZonedDateTime lastSnapTimestamp = ZonedDateTime.now();

//    public AggregateRoot() {
//        // Preconditions.checkNotNull(id,"aggregate id not allowed to be empty");
//    }
//
//    public AggregateRoot(Long id) {
//        Preconditions.checkNotNull(id, "aggregate id not allowed to be empty");
//        setId(id);
//    }

    public final int getVersion() {
        return version;
    }

    public final void setVersion(int baseVersion) {
        this.version = baseVersion;
    }

    public ZonedDateTime getTimestamp() {
        return timestamp;
    }

    public void setTimestamp(ZonedDateTime timestamp) {
        this.timestamp = timestamp;
    }

    public ZonedDateTime getLastSnapTimestamp() {
        return lastSnapTimestamp;
    }

    public void setLastSnapTimestamp(ZonedDateTime lastSnapTimestamp) {
        this.lastSnapTimestamp = lastSnapTimestamp;
    }

    private void appendUncommittedEvent(Event event) {
        if (uncommittedEvents == null) {
            uncommittedEvents = new ConcurrentLinkedQueue<>();
        }
//        if (uncommittedEvents.stream().anyMatch(x -> x.getClass().equals(event.getClass()))) {
//            throw new UnsupportedOperationException(
//                    String.format("Cannot apply duplicated domain event type: %s, current aggregateRoot type: %s, id: %s",
//                            event.getClass(),
//                            this.getClass().getName(),
//                            id)
//            );
//        }
        uncommittedEvents.add(event);
    }

    /**
     * 聚合根事件触发
     *
     * @param event
     */
    protected void applyNewEvent(Event event) {
        apply(event);
        event.setVersion(getVersion() + 1);
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
                    String.format("Aggregate '%s' doesn't apply event type '%s'", this.getClass(), event.getClass()),
                    e
            );
        }
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
     */
    public void acceptChanges() {
        if (uncommittedEvents != null && !uncommittedEvents.isEmpty()) {
            setVersion(uncommittedEvents.peek().getVersion());
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
                            event.getAggregateId(),
                            this.getId(),
                            this.getClass().getName())
            );
        }
        if (event.getVersion() != getVersion() + 1) {
            throw new UnsupportedOperationException(String.format(
                    "Invalid domain event stream, version: %d, expected version: %d, current aggregateRoot type: %s, id: %s",
                    event.getVersion(),
                    getVersion(),
                    this.getClass().getName(),
                    getId())
            );
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
        });
        setVersion(events.stream().findFirst().get().getVersion());
        ZonedDateTime now = ZonedDateTime.now();
        setTimestamp(now);
        setLastSnapTimestamp(now);
    }

    public abstract Long getId();

    public abstract void setId(Long id);

    /**
     * 判断是否达到聚合根的快照周期
     *
     * @return
     */
    public Boolean isSnapshotCycle(Long snapshotCycle) {
        long updateTime = timestamp.toInstant().getEpochSecond();
        long snapTime = lastSnapTimestamp.toInstant().getEpochSecond();
        return snapshotCycle > 0 && (updateTime - snapTime) >= snapshotCycle;
    }

}
