package com.damon.cqrs.sample.train.aggregate.value_object;

public class TicketBuyStatus {

    private final TICKET_BUY_STATUS stauts;

    private Integer seatIndex;

    public TicketBuyStatus(TICKET_BUY_STATUS stauts, Integer seatIndex) {
        this.stauts = stauts;
        this.seatIndex = seatIndex;
    }

    public TicketBuyStatus(TICKET_BUY_STATUS stauts) {
        this.stauts = stauts;
    }

    public Integer getSeatIndex() {
        return seatIndex;
    }

    public void setSeatIndex(Integer seatIndex) {
        this.seatIndex = seatIndex;
    }

    public TICKET_BUY_STATUS getStauts() {
        return stauts;
    }
}
