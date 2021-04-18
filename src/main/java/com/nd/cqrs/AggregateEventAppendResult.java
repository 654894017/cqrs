package com.nd.cqrs;

import java.util.List;

import com.google.common.collect.Lists;

import lombok.Data;

@Data
public class AggregateEventAppendResult {

    private AggregateGroup group;

    private EventAppendStatus eventAppendStatus;

    private List<String> duplicateCommandIds = Lists.newArrayList();
    
    private Throwable throwable;

    

}
