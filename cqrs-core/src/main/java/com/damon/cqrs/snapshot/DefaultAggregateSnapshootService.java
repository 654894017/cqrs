package com.damon.cqrs.snapshot;

import com.damon.cqrs.CqrsApplicationContext;
import com.damon.cqrs.command.ICommandService;
import com.damon.cqrs.domain.AggregateRoot;
import com.damon.cqrs.utils.NamedThreadFactory;
import lombok.extern.slf4j.Slf4j;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.concurrent.*;
import java.util.concurrent.locks.ReentrantLock;

/**
 * 聚合快照保存处理服务（默认定时提交聚合快照）
 *
 * @author xianping_lu
 */
@Slf4j
public class DefaultAggregateSnapshootService implements IAggregateSnapshootService {

    private final int aggregateSnapshootProcessThreadNumber;

    private final int delaySeconds;

    private final ExecutorService aggregateSnapshootService;

    private final ScheduledExecutorService scheduledExecutorService;

    private final List<LinkedBlockingQueue<AggregateRoot>> queueList = new ArrayList<>();

    private final ReentrantLock lock = new ReentrantLock(true);

    private final HashMap<Long, AggregateRoot> map = new HashMap<>();

    public DefaultAggregateSnapshootService(final int aggregateSnapshootProcessThreadNumber, final int delaySeconds) {
        this(aggregateSnapshootProcessThreadNumber, delaySeconds, 4 * 1024);
    }

    public DefaultAggregateSnapshootService(final int aggregateSnapshootProcessThreadNumber, final int delaySeconds, int queueSize) {
        this.aggregateSnapshootService = Executors.newFixedThreadPool(aggregateSnapshootProcessThreadNumber, new NamedThreadFactory("aggregate-snapshoot-pool"));
        this.scheduledExecutorService = Executors.newSingleThreadScheduledExecutor();
        this.delaySeconds = delaySeconds;
        for (int number = 0; number < aggregateSnapshootProcessThreadNumber; number++) {
            this.queueList.add(new LinkedBlockingQueue<>(queueSize));
        }
        this.aggregateSnapshootProcessThreadNumber = aggregateSnapshootProcessThreadNumber;
        this.processingAggregateSnapshoot();
        this.startScheule();
    }

    private void startScheule() {
        scheduledExecutorService.scheduleWithFixedDelay(() -> {
            lock.lock();
            try {
                Collection<AggregateRoot> aggregates = map.values();
                aggregates.forEach(aggregate -> {
                    if (!getQueue(aggregate.getId()).offer(aggregate)) {
                        log.warn("aggregate snapshoot handle queue is full. aggregateId : {}, type : {}", aggregate.getId(), aggregate.getClass().getTypeName());
                    }
                });
                map.clear();
            } catch (Throwable e) {
                log.error("aggregate snapshoot enqueue failed. ", e);
            } finally {
                lock.unlock();
            }
        }, 5, delaySeconds, TimeUnit.SECONDS);
    }

    private LinkedBlockingQueue<AggregateRoot> getQueue(Long aggregateId) {
        int hash = aggregateId.hashCode();
        if (hash < 0) {
            hash = Math.abs(hash);
        }
        int index = hash % aggregateSnapshootProcessThreadNumber;
        return queueList.get(index);
    }

    /**
     * @param snapshoot
     */
    @Override
    public void saveAggregateSnapshot(AggregateRoot snapshoot) {
        lock.lock();
        try {
            map.put(snapshoot.getId(), snapshoot);
        } finally {
            lock.unlock();
        }
    }

    private void processingAggregateSnapshoot() {
        for (int number = 0; number < aggregateSnapshootProcessThreadNumber; number++) {
            final int num = number;
            aggregateSnapshootService.submit(() -> {
                for (; ; ) {
                    try {
                        AggregateRoot aggregate = queueList.get(num).take();
                        ICommandService<AggregateRoot> commandService = CqrsApplicationContext.get(aggregate.getClass().getTypeName());
                        commandService.saveAggregateSnapshot(aggregate);
                    } catch (Throwable e) {
                        log.error("aggregate snapshoot save failed", e);
                    }
                }
            });
        }
    }
}
