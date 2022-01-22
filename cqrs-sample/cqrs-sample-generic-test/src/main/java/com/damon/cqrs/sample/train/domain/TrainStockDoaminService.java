package com.damon.cqrs.sample.train.domain;


import com.damon.cqrs.AbstractDomainService;
import com.damon.cqrs.event.EventCommittingService;
import com.damon.cqrs.sample.train.command.*;
import com.damon.cqrs.sample.train.dto.TrainStockDTO;

import java.util.BitSet;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentSkipListMap;

public class TrainStockDoaminService extends AbstractDomainService<TrainStock> {

    public TrainStockDoaminService(EventCommittingService eventCommittingService) {
        super(eventCommittingService);
    }

    public void createTrain(TrainCreateCommand command) {
        super.process(command, () ->
                new TrainStock(command.getAggregateId(), command.getS2s(), command.getSeatCount())
        ).join();
    }


    public TrainStockDTO getTrain(TicketGetCommand command) {
        return super.process(command, ts -> {
            TrainStockDTO stock = new TrainStockDTO();
            stock.setId(ts.getId());
            ConcurrentSkipListMap<Integer, BitSet> s2sSeatCount = ts.getS2sSeatCount();
            ConcurrentSkipListMap<Integer, Integer> s2ssc = new ConcurrentSkipListMap<>();
            s2sSeatCount.forEach((key, set) -> {
                s2ssc.put(key, ts.getSeatCount() - set.cardinality());
            });
            stock.setS2sSeatCount(s2ssc);
            return stock;
        }).join();
    }

    public TrainStock.S2S_TICKET_PROTECT_STATUS protectTicket(TicketProtectCommand command) {
        return super.process(command, ts ->
                ts.protectS2STicket(command)
        ).join();
    }

    public TrainStock.STATION_TICKET_LIMIT_STATUS limitStationTicket(StationTicketLimitCommand command) {
        return super.process(command, ts ->
                ts.limitStationTicket(command)
        ).join();
    }
    public TrainStock.TICKET_PROTECT_CANCEL_STATUS cancelProtectTicket(TicketProtectCancelCommand command) {
        return super.process(command, ts ->
                ts.cancelProtectTicket(command)
        ).join();
    }

    public TrainStock.TicketBuyStatus buyTicket(TicketBuyCommand command) {
        return super.process(command, ts ->
                ts.buyTicket(command)
        ).join();
    }

    public TrainStock.TICKET_CANCEL_STAUTS cancelTicket(TicketCancelCommand command) {
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

