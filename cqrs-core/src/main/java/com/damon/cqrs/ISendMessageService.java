package com.damon.cqrs;

import java.util.List;

public interface ISendMessageService {

    void sendMessage(List<EventSendingContext> contexts);

}
