package com.damon.cqrs.event;

import com.damon.cqrs.AggregateGroup;
import com.google.common.collect.Lists;
import lombok.Data;

import java.util.List;

@Data
public class AggregateEventAppendResult {

    private AggregateGroup group;

    private EventAppendStatus eventAppendStatus;

    private List<String> duplicateCommandIds = Lists.newArrayList();

    private Throwable throwable;


}
