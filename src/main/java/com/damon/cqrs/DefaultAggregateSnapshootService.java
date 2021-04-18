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

import com.damon.cqrs.domain.Aggregate;
import com.damon.cqrs.domain.DomainService;

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

    private Object lock = new Object();

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
            try {
                Collection<Aggregate> aggregates;
                synchronized (lock) {
                    aggregates = map.values();
                    map.clear();
                }
                aggregates.forEach(aggregate -> {
                    int index = (int) (Math.abs(aggregate.getId()) % aggregateSnapshootProcessThreadNumber);
                    queueList.get(index).add(aggregate);
                });

            } catch (Throwable e) {
                log.error("aggregate snapshoot enqueue failture. ", e);
            }

        }, 5, delaySeconds, TimeUnit.SECONDS);
    }

    /**
     * 使用同步锁代码简单（暂时没想到更好的处理方式）
     * 
     * @param aggregateSnapshoot
     */
    @Override
    public void addAggregategetSnapshoot(Aggregate aggregateSnapshoot) {
        synchronized (lock) {
            map.put(aggregateSnapshoot.getId(), aggregateSnapshoot);
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
