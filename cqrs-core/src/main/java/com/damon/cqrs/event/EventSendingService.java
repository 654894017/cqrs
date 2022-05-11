package com.damon.cqrs.event;

import com.damon.cqrs.utils.NamedThreadFactory;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

/**
 * 领域事件发送服务
 *
 * @author xianping_lu
 */
public class EventSendingService {
    private final List<EventSendingMailBox> mailBoxs;
    private final ExecutorService service;
    private final ISendMessageService sendMessageService;
    private final int mailboxNumber;

    public EventSendingService(ISendMessageService sendMessageService, int mailBoxNumber, int batchSize) {
        mailBoxs = new ArrayList<EventSendingMailBox>(mailBoxNumber);
        service = Executors.newFixedThreadPool(mailBoxNumber, new NamedThreadFactory("event-sending-pool"));
        this.mailboxNumber = mailBoxNumber;
        this.sendMessageService = sendMessageService;
        for (int number = 0; number < mailBoxNumber; number++) {
            mailBoxs.add(new EventSendingMailBox(service, contexts -> batchSendEventAsync(contexts), number, batchSize));
        }
    }

    /**
     * 发送聚合事件到消息中间件
     *
     * @param event
     */
    public void sendDomainEventAsync(EventSendingContext event) {
        Long aggregateId = event.getAggregateId();
        int hash = aggregateId.hashCode();
        if (hash < 0) {
            hash = Math.abs(hash);
        }
        int index = hash % mailboxNumber;
        EventSendingMailBox maxibox = mailBoxs.get(index);
        maxibox.enqueue(event);
    }


    private void batchSendEventAsync(List<EventSendingContext> contexts) {
        sendMessageService.sendMessage(contexts);
    }

}
