package com.damon.cqrs.sample.trade_matching.aggregate;

import com.damon.cqrs.domain.AggregateRoot;
import com.damon.cqrs.sample.trade_matching.cmd.StockBuyCmd;
import com.damon.cqrs.sample.trade_matching.cmd.StockMatchCmd;
import com.damon.cqrs.sample.trade_matching.cmd.StockSellCmd;
import com.damon.cqrs.sample.trade_matching.event.StockBoughtEvent;
import com.damon.cqrs.sample.trade_matching.event.StockBuyEntrustSucceedEvent;
import com.damon.cqrs.sample.trade_matching.event.StockSelleEntrustSucceedEvent;
import com.damon.cqrs.sample.trade_matching.event.StockSelledEvent;
import lombok.Getter;
import lombok.Setter;

import java.util.HashMap;
import java.util.Map;
import java.util.PriorityQueue;

@Getter
@Setter
public class Stock extends AggregateRoot {
    private Map<Long, Boolean> tradeMap = new HashMap<>();
    private PriorityQueue<StockBuyOrder> buyOrderPriorityQueue = new PriorityQueue<>((o1, o2) -> {
        // 先比较价格，如果价格相同，则比较创建时间
        int priceComparison = o2.getPrice().compareTo(o1.getPrice());
        if (priceComparison != 0) {
            // 高价格优先
            return priceComparison;
        } else {
            // 早创建的优先
            return o1.getCreateTime().compareTo(o2.getCreateTime());
        }
    });
    private PriorityQueue<StockSellOrder> sellOrderPriorityQueue = new PriorityQueue<>((o1, o2) -> {
        // 先比较价格，如果价格相同，则比较创建时间
        int priceComparison = o1.getPrice().compareTo(o2.getPrice());
        if (priceComparison != 0) {
            // 低价格优先
            return priceComparison;
        } else {
            // 早创建的优先
            return o1.getCreateTime().compareTo(o2.getCreateTime());
        }
    });

    public Stock(Long id) {
        super(id);
    }

    private void apply(StockBuyEntrustSucceedEvent event) {
        buyOrderPriorityQueue.offer(event.getBuyOrder());
        tradeMap.put(event.getBuyOrder().getOrderId(), true);
    }

    private void apply(StockSelleEntrustSucceedEvent event) {
        sellOrderPriorityQueue.offer(event.getSellOrder());
        tradeMap.put(event.getSellOrder().getOrderId(), true);
    }

    public int sell(StockSellCmd cmd) {
        Boolean isTrade = tradeMap.get(cmd.getOrderId());
        if (isTrade == null) {
            applyNewEvent(new StockSelleEntrustSucceedEvent(new StockSellOrder(cmd.getPrice(), System.nanoTime(), cmd.getNumber(), cmd.getOrderId())));
            return 0;
        } else {
            return -1;
        }
    }

    private void apply(StockSelledEvent event) {
        if (event.isFinished()) {
            sellOrderPriorityQueue.poll();
        } else {
            StockSellOrder sellOrder = sellOrderPriorityQueue.peek();
            sellOrder.deduction(event.getTradingNumber());
        }
    }

    private void apply(StockBoughtEvent event) {
        if (event.isFinished()) {
            buyOrderPriorityQueue.poll();
        } else {
            StockBuyOrder buyOrder = buyOrderPriorityQueue.peek();
            buyOrder.deduction(event.getTradingNumber());
        }
    }

    public int match(StockMatchCmd cmd) {
        StockBuyOrder buyOrder = buyOrderPriorityQueue.peek();
        if (buyOrder == null) {
            return -1;
        }
        StockSellOrder sellOrder = sellOrderPriorityQueue.peek();
        if (sellOrder == null) {
            return -1;
        }
        if (buyOrder.getPrice() < sellOrder.getPrice()) {
            return -1;
        }
        if (buyOrder.getNumber() < sellOrder.getNumber()) {
            applyNewEvent(new StockSelledEvent(
                    getId(), sellOrder.getPrice(), sellOrder.getOrderId(), sellOrder.getNumber(), buyOrder.getNumber(), false
            ));
            applyNewEvent(new StockBoughtEvent(
                    getId(), sellOrder.getPrice(), buyOrder.getOrderId(), buyOrder.getNumber(), buyOrder.getNumber(), true
            ));
        } else if (buyOrder.getNumber().equals(sellOrder.getNumber())) {
            applyNewEvent(new StockSelledEvent(
                    getId(), sellOrder.getPrice(), sellOrder.getOrderId(), sellOrder.getNumber(), sellOrder.getNumber(), true
            ));
            applyNewEvent(new StockBoughtEvent(
                    getId(), buyOrder.getPrice(), buyOrder.getOrderId(), buyOrder.getNumber(), buyOrder.getNumber(), true
            ));
        } else {
            applyNewEvent(new StockSelledEvent(
                    getId(), sellOrder.getPrice(), sellOrder.getOrderId(), sellOrder.getNumber(), sellOrder.getNumber(), true
            ));
            applyNewEvent(new StockBoughtEvent(
                    getId(), buyOrder.getPrice(), buyOrder.getOrderId(), buyOrder.getNumber(), buyOrder.getNumber(), false
            ));
        }
        return 0;

    }

    public int buy(StockBuyCmd cmd) {
        Boolean isTrade = tradeMap.get(cmd.getOrderId());
        if (isTrade == null) {
            applyNewEvent(new StockBuyEntrustSucceedEvent(new StockBuyOrder(cmd.getPrice(), System.nanoTime(), cmd.getNumber(), cmd.getOrderId())));
            return 0;
        } else {
            return -1;
        }
    }
}
