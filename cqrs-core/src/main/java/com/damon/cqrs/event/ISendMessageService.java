package com.damon.cqrs.event;

import java.util.List;

public interface ISendMessageService {

    void sendMessage(List<EventSendingContext> contexts);

}
