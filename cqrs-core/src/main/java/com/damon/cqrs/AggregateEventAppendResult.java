package com.damon.cqrs;

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
