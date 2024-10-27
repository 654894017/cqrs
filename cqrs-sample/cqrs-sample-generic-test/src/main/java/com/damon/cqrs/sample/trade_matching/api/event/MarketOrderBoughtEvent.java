package com.damon.cqrs.sample.trade_matching.api.event;

import com.damon.cqrs.domain.Event;
import lombok.Data;

import java.util.Set;

@Data
public class MarketOrderBoughtEvent extends Event {
    private Long stockId;
    private Long orderId;
    private Integer totalNumber;
    private Set<TradeOrder> tradeOrders;
    /**
     * 1 最优5档成交剩余撤销 0 最优5档成交剩余转限价单
     */
    private int entrustType;
    public MarketOrderBoughtEvent() {
    }
    public MarketOrderBoughtEvent(Long orderId, Set<TradeOrder> tradeOrders, Long stockId, Integer totalNumber, int entrustType) {
        this.orderId = orderId;
        this.tradeOrders = tradeOrders;
        this.stockId = stockId;
        this.totalNumber = totalNumber;
        this.entrustType = entrustType;
    }

    public Integer undoneNumber() {
        return totalNumber - tradeOrders.stream().mapToInt(TradeOrder::getNumber).sum();
    }

    public boolean isDone() {
        return undoneNumber() == 0;
    }

    @Data
    public static class TradeOrder {
        private Long sellerOrderId;
        private Integer number;
        private Long price;
        private boolean isDone;
        public TradeOrder(Long sellerOrderId, boolean isDone, Integer number, Long price) {
            this.isDone = isDone;
            this.number = number;
            this.price = price;
            this.sellerOrderId = sellerOrderId;
        }
        public TradeOrder() {
        }
    }

}
