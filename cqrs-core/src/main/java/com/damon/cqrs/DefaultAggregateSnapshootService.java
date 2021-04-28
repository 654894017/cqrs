package com.damon.cqrs;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.locks.ReentrantLock;

import com.domain.cqrs.domain.Aggregate;

import lombok.extern.slf4j.Slf4j;

/**
 * 
 * 聚合快照保存处理服务（默认定时提交聚合快照）
 * 
 * 
 * 
 * @author xianping_lu
 *
 */
@Slf4j
public class DefaultAggregateSnapshootService implements IAggregateSnapshootService {

    private int aggregateSnapshootProcessThreadNumber;

    private ExecutorService service;

    private ScheduledExecutorService scheduledExecutorService;

    private ConcurrentHashMap<Long, Aggregate> map;

    private List<LinkedBlockingQueue<Aggregate>> queueList = new ArrayList<>();

    private ReentrantLock lock = new ReentrantLock(true);

    public DefaultAggregateSnapshootService(final int aggregateSnapshootProcessThreadNumber, final int delaySeconds) {
        this.service = Executors.newFixedThreadPool(aggregateSnapshootProcessThreadNumber);
        this.scheduledExecutorService = Executors.newSingleThreadScheduledExecutor();
        this.map = new ConcurrentHashMap<>();
        for (int number = 0; number < aggregateSnapshootProcessThreadNumber; number++) {
            this.queueList.add(new LinkedBlockingQueue<Aggregate>());
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
                map.clear();
            } catch (Throwable e) {
                log.error("aggregate snapshoot enqueue failture. ", e);
            } finally {
                lock.unlock();
            }
        }, 5, delaySeconds, TimeUnit.SECONDS);
    }

    /**
     * 使用重入锁代码简单（暂时没想到更好的处理方式）
     * 
     * @param aggregateSnapshoot
     */
    @Override
    public void addAggregategetSnapshoot(Aggregate aggregateSnapshoot) {
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
                        DomainService<Aggregate> domainService = AggregateOfDomainServiceMap.get(aggregate.getClass().getTypeName());
                        domainService.saveAggregateSnapshoot(aggregate);
                    } catch (Throwable e) {
                        log.error("aggregate snapshoot save failture ", e);
                    }
                }
            });
        }
    }

}
