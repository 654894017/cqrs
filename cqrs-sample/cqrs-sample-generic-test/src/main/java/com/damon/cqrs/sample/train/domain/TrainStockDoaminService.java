package com.damon.cqrs.sample.train.domain;


import com.damon.cqrs.AbstractDomainService;
import com.damon.cqrs.event.EventCommittingService;
import com.damon.cqrs.sample.train.command.TicketBuyCommand;
import com.damon.cqrs.sample.train.command.TicketCancelCommand;
import com.damon.cqrs.sample.train.command.TicketGetCommand;
import com.damon.cqrs.sample.train.command.TrainCreateCommand;
import com.damon.cqrs.utils.BeanMapper;

import java.util.concurrent.CompletableFuture;

public class TrainStockDoaminService extends AbstractDomainService<TrainStock> {

    public TrainStockDoaminService(EventCommittingService eventCommittingService) {
        super(eventCommittingService);
    }

    public void createTrain(TrainCreateCommand command) {
        super.process(command, () ->
                new TrainStock(command.getAggregateId(), command.getS2sSeatCount())
        ).join();
    }

    public TrainStock getTrain(TicketGetCommand command) {
        return super.process(command, ts ->
                BeanMapper.map(ts, TrainStock.class)
        ).join();
    }

    public int buyTicket(TicketBuyCommand command) {
        return super.process(command, ts ->
                ts.buyTicket(command)
        ).join();
    }

    public int cancelTicket(TicketCancelCommand command) {
        return super.process(command, ts ->
                ts.cancelTicket(command)
        ).join();
    }

    @Override
    public CompletableFuture<TrainStock> getAggregateSnapshoot(long aggregateId, Class<TrainStock> classes) {
        return CompletableFuture.completedFuture(null);
    }

    @Override
    public CompletableFuture<Boolean> saveAggregateSnapshoot(TrainStock aggregate) {
        return CompletableFuture.completedFuture(true);
    }


}

