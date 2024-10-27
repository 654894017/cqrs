package com.damon.cqrs.sample.trade_matching.api.event;

import com.damon.cqrs.domain.Event;
import lombok.Data;

import java.util.Set;

@Data
public class MarketOrderSelledEvent extends Event {
    private Long stockId;
    private Long orderId;
    private Integer totalNumber;
    private Set<TradeOrder> tradeOrders;
    /**
     * 1 最优5档成交剩余撤销 0 最优5档成交剩余转限价单
     */
    private int entrustmentType;

    public MarketOrderSelledEvent() {
    }

    public MarketOrderSelledEvent(Long orderId, Set<TradeOrder> tradeOrders, Long stockId, Integer totalNumber, int entrustmentType) {
        this.orderId = orderId;
        this.tradeOrders = tradeOrders;
        this.stockId = stockId;
        this.totalNumber = totalNumber;
        this.entrustmentType = entrustmentType;
    }

    public Integer undoneNumber() {
        return totalNumber - tradeOrders.stream().mapToInt(TradeOrder::getNumber).sum();
    }

    public boolean isTransferLimitOrderEntrustment() {
        return entrustmentType == 1;
    }

    public boolean isDone() {
        return undoneNumber() == 0;
    }

    public boolean isUndone() {
        return !isDone();
    }

    @Data
    public static class TradeOrder {
        private Long buyerOrderId;
        private Integer number;
        private Long price;
        private boolean isDone;

        public TradeOrder(Long buyerOrderId, boolean isDone, Integer number, Long price) {
            this.isDone = isDone;
            this.number = number;
            this.price = price;
            this.buyerOrderId = buyerOrderId;
        }

        public TradeOrder() {
        }
    }

}
