package com.damon.cqrs;

import com.damon.cqrs.domain.Event;

import java.util.List;
import java.util.Map;

/**
 * 领域事件监听器
 *
 * @author xianpinglu
 */
public interface IEventListener {
    /**
     * 处理领域事件
     *
     * 注意：当前事件已经分组排序，业务可以考虑批量处理。
     * @param events
     */
    void process(Map<String, List<Event>> events);
}
