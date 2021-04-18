package com.nd.cqrs;

import java.util.List;

public interface ISendMessageService {

    void sendMessage(List<EventSendingContext> contexts);

}
