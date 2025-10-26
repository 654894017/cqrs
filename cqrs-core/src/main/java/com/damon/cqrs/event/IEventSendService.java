package com.damon.cqrs.event;

import java.util.List;

public interface IEventSendService {

    void sendMessage(List<EventSendingContext> contexts);

}
