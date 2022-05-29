package com.damon.cqrs;

import com.damon.cqrs.domain.AggregateRoot;
import com.damon.cqrs.event.CQRSContext;
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

    private final int AGGREGATE_SNAPSHOT_QUEUE_SIZE = 1 * 2048;

    private final int aggregateSnapshootProcessThreadNumber;

    private final ExecutorService aggregateSnapshootService;

    private final ScheduledExecutorService scheduledExecutorService;

    private final List<LinkedBlockingQueue<AggregateRoot>> queueList = new ArrayList<>();

    private final ReentrantLock lock = new ReentrantLock(true);

    private final HashMap<Long, AggregateRoot> map = new HashMap<>();

    public DefaultAggregateSnapshootService(final int aggregateSnapshootProcessThreadNumber, final int delaySeconds) {
        this.aggregateSnapshootService = Executors.newFixedThreadPool(aggregateSnapshootProcessThreadNumber, new NamedThreadFactory("aggregate-snapshoot-pool"));
        this.scheduledExecutorService = Executors.newSingleThreadScheduledExecutor();
        for (int number = 0; number < aggregateSnapshootProcessThreadNumber; number++) {
            this.queueList.add(new LinkedBlockingQueue<>(AGGREGATE_SNAPSHOT_QUEUE_SIZE));
        }
        this.aggregateSnapshootProcessThreadNumber = aggregateSnapshootProcessThreadNumber;
        this.processingAggregateSnapshoot();
        scheduledExecutorService.scheduleWithFixedDelay(() -> {
            lock.lock();
            try {
                Collection<AggregateRoot> aggregates = map.values();
                aggregates.forEach(aggregate -> {
                    int hash = aggregate.getId().hashCode();
                    if (hash < 0) {
                        hash = Math.abs(hash);
                    }
                    int index = hash % aggregateSnapshootProcessThreadNumber;
                    if (!queueList.get(index).offer(aggregate)) {
                        log.error("aggregate snapshoot handle queue is full. aggregateId : {}, type : {}", aggregate.getId(), aggregate.getClass().getTypeName());
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

    /**
     * @param aggregateSnapshoot
     */
    @Override
    public void saveAggregategetSnapshot(AggregateRoot aggregateSnapshoot) {
        lock.lock();
        try {
            map.put(aggregateSnapshoot.getId(), aggregateSnapshoot);
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
                        AbstractDomainService<AggregateRoot> domainService = CQRSContext.get(aggregate.getClass().getTypeName());
                        domainService.saveAggregateSnapshot(aggregate);
                    } catch (Throwable e) {
                        log.error("aggregate snapshoot save failed", e);
                    }
                }
            });
        }
    }
}
