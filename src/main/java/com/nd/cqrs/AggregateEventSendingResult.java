package com.nd.cqrs;

import lombok.Data;

@Data
public class AggregateEventSendingResult {

    private long aggregateId;

    private EventSendingStatus eventSendingStatus;

    private Throwable throwable;

}
