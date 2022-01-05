package com.damon.cqrs;

import com.damon.cqrs.domain.Aggregate;
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

    private final ExecutorService service;

    private final ScheduledExecutorService scheduledExecutorService;
    private final List<LinkedBlockingQueue<Aggregate>> queueList = new ArrayList<>();
    private final ReentrantLock lock = new ReentrantLock(true);
    private HashMap<Long, Aggregate> map = new HashMap<>();

    public DefaultAggregateSnapshootService(final int aggregateSnapshootProcessThreadNumber, final int delaySeconds) {
        this.service = Executors.newFixedThreadPool(aggregateSnapshootProcessThreadNumber);
        this.scheduledExecutorService = Executors.newSingleThreadScheduledExecutor();
        for (int number = 0; number < aggregateSnapshootProcessThreadNumber; number++) {
            this.queueList.add(new LinkedBlockingQueue<Aggregate>(1024));
        }
        this.aggregateSnapshootProcessThreadNumber = aggregateSnapshootProcessThreadNumber;
        processingAggregateSnapshoot();
        scheduledExecutorService.scheduleWithFixedDelay(() -> {
            lock.lock();
            try {
                Collection<Aggregate> aggregates = map.values();
                aggregates.parallelStream().forEach(aggregate -> {
                    int index = (int) (Math.abs(aggregate.getId()) % aggregateSnapshootProcessThreadNumber);
                    queueList.get(index).add(aggregate);
                });
                map = new HashMap<Long, Aggregate>(map.size());
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
    public void saveAggregategetSnapshoot(Aggregate aggregateSnapshoot) {
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
            service.submit(() -> {
                while (true) {
                    try {
                        Aggregate aggregate = queueList.get(num).take();
                        AbstractDomainService<Aggregate> domainService = DomainServiceContext.get(aggregate.getClass().getTypeName());
                        domainService.saveAggregateSnapshoot(aggregate);
                    } catch (Throwable e) {
                        log.error("aggregate snapshoot save failed", e);
                    }
                }
            });
        }
    }
}
