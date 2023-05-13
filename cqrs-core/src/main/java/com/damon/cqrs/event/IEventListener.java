package com.damon.cqrs.event;

import com.damon.cqrs.domain.Event;

import java.util.List;
import java.util.Map;

/**
 * 领域事件监听器
 *
 * @param <P> 分区编号
 * @author xianpinglu
 */
public interface IEventListener {

    /**
     * 处理领域事件
     * <p>
     * 注意：当前事件已经按照分区进行分组，业务可以考虑批量处理。
     *
     * @param aggregateEventGroup
     */
    void process(Map<Integer, List<List<Event>>> aggregateEventGroup);
}
