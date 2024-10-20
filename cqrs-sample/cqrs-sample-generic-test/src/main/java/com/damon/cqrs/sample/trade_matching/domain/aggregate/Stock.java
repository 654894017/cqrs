package com.damon.cqrs.sample.trade_matching.domain.aggregate;

import com.damon.cqrs.domain.AggregateRoot;
import com.damon.cqrs.sample.trade_matching.api.cmd.StockBuyCmd;
import com.damon.cqrs.sample.trade_matching.api.cmd.StockOrderCancelCmd;
import com.damon.cqrs.sample.trade_matching.api.cmd.StockOrderMatchCmd;
import com.damon.cqrs.sample.trade_matching.api.cmd.StockSellCmd;
import com.damon.cqrs.sample.trade_matching.api.event.*;
import lombok.Getter;
import lombok.Setter;

import java.util.*;
import java.util.concurrent.ConcurrentSkipListMap;

@Getter
@Setter
public class Stock extends AggregateRoot {
    private Long realtimePrice = 100L;
    private Long notchPrice = 1L;
    private Map<Long, Boolean> tradeMap = new HashMap<>();
    private TreeMap<Long, ConcurrentSkipListMap<Long, StockBuyOrder>> buyOrderMap = new TreeMap<>(Comparator.reverseOrder());
    private TreeMap<Long, ConcurrentSkipListMap<Long, StockSellOrder>> sellOrderMap = new TreeMap<>();

    public Stock(Long id) {
        super(id);
    }

    public int sell(StockSellCmd cmd) {
        Boolean isTrade = tradeMap.get(cmd.getOrderId());
        if (isTrade == null) {
            applyNewEvent(new StockSelleEntrustSucceedEvent(
                    new StockSellOrder(cmd.getPrice(), System.nanoTime(), cmd.getNumber(), cmd.getOrderId(), cmd.getType())
            ));
            return 0;
        } else {
            return -1;
        }
    }

    public int cancel(StockOrderCancelCmd cmd) {
        Boolean isTrade = tradeMap.get(cmd.getOrderId());
        if (isTrade == null) {
            return -1;
        } else {
            if (cmd.isBuyOrder()) {
                ConcurrentSkipListMap<Long, StockBuyOrder> orderMap = buyOrderMap.get(cmd.getPrice());
                if (orderMap != null && orderMap.containsKey(cmd.getOrderId())) {
                    applyNewEvent(new StockOrderCancelledEvent(cmd.getOrderId(), 1, cmd.getPrice()));
                }
            } else {
                ConcurrentSkipListMap<Long, StockSellOrder> orderMap = sellOrderMap.get(cmd.getPrice());
                if (orderMap != null && orderMap.containsKey(cmd.getOrderId())) {
                    applyNewEvent(new StockOrderCancelledEvent(cmd.getOrderId(), 0, cmd.getPrice()));
                }
            }
            return 0;
        }
    }

    public int buy(StockBuyCmd cmd) {
        if (1 == 1) {
            throw new RuntimeException("a1");
        }
        Boolean isTrade = tradeMap.get(cmd.getOrderId());
        if (isTrade == null) {
            applyNewEvent(new StockBuyEntrustSucceedEvent(
                    new StockBuyOrder(cmd.getPrice(), System.nanoTime(), cmd.getNumber(), cmd.getOrderId(), cmd.getType())
            ));
            return 0;
        } else {
            return -1;
        }
    }

    public int match(StockOrderMatchCmd cmd) {
        SortedMap<Long, ConcurrentSkipListMap<Long, StockBuyOrder>> sortedBuyOrderMap = buyOrderMap.headMap(realtimePrice, true);
        if (sortedBuyOrderMap.isEmpty()) {
            return -1;
        }
        SortedMap<Long, ConcurrentSkipListMap<Long, StockSellOrder>> sortedSellOrderMap = sellOrderMap.headMap(realtimePrice, true);

        Map.Entry<Long, ConcurrentSkipListMap<Long, StockSellOrder>> lowestSellOrderEntry = findFirstNonEmptySellOrder();
        if (lowestSellOrderEntry == null) {
            return -1;
        }
        Long sellOrderLowestPrice = lowestSellOrderEntry.getKey();
        // 遍历买单并尝试匹配卖单
        for (Map.Entry<Long, ConcurrentSkipListMap<Long, StockBuyOrder>> buyEntry : sortedBuyOrderMap.entrySet()) {
            ConcurrentSkipListMap<Long, StockBuyOrder> buyOrders = buyEntry.getValue();
            for (StockBuyOrder buyOrder : buyOrders.values()) {
                StockSellOrder sellOrder = lowestSellOrderEntry.getValue().firstEntry().getValue();
                // 检查限价单或市价单的匹配条件
                if (isOrderMatchable(buyOrder, sellOrderLowestPrice)) {
                    applyEvent(buyOrder, sellOrder);
                    return 0;
                }
            }
        }
        return -1;
    }

    public SortedMap<Long, ConcurrentSkipListMap<Long, StockBuyOrder>> findTopNBuyOrder(int notch) {
        SortedMap<Long, ConcurrentSkipListMap<Long, StockBuyOrder>> sortedMap = new TreeMap<>();
        for (Map.Entry<Long, ConcurrentSkipListMap<Long, StockBuyOrder>> entry : buyOrderMap.entrySet()) {
            ConcurrentSkipListMap<Long, StockBuyOrder> buyOrders = entry.getValue();
            if (buyOrders != null && !buyOrders.isEmpty()) {
                sortedMap.put(entry.getKey(), buyOrders);
                if (sortedMap.size() == notch) {
                    return sortedMap;
                }
            }
        }
        return sortedMap;
    }

    public Map.Entry<Long, ConcurrentSkipListMap<Long, StockSellOrder>> findFirstNonEmptySellOrder() {
        for (Map.Entry<Long, ConcurrentSkipListMap<Long, StockSellOrder>> entry : sellOrderMap.entrySet()) {
            ConcurrentSkipListMap<Long, StockSellOrder> sellOrders = entry.getValue();
            if (sellOrders != null && !sellOrders.isEmpty()) {
                return entry;
            }
        }
        return null;
    }

    private boolean isOrderMatchable(StockBuyOrder buyOrder, Long sellOrderLowestPrice) {
        // 限价单：买单价格需要大于等于卖单最低价格
        if (buyOrder.isLimitOrder()) {
            return sellOrderLowestPrice <= buyOrder.getPrice();
        }
        // 市价单：无需比较价格，只要有卖单即可
        return sellOrderLowestPrice <= buyOrder.getPrice();
    }

    private void applyEvent(StockBuyOrder buyOrder, StockSellOrder sellOrder) {
        boolean isSellFinished = sellOrder.getNumber() <= buyOrder.getNumber();
        boolean isBuyFinished = sellOrder.getNumber() >= buyOrder.getNumber();
        applyNewEvent(new StockSelledEvent(
                getId(), sellOrder.getPrice(), sellOrder.getOrderId(),
                sellOrder.getNumber(), Math.abs(buyOrder.getNumber() - sellOrder.getNumber()), isSellFinished
        ));
        applyNewEvent(new StockBoughtEvent(
                getId(), buyOrder.getPrice(), buyOrder.getOrderId(),
                buyOrder.getNumber(), Math.abs(buyOrder.getNumber() - sellOrder.getNumber()), isBuyFinished
        ));
    }

    private void apply(StockOrderCancelledEvent event) {
        if (event.getType() == 1) {
            ConcurrentSkipListMap<Long, StockBuyOrder> orderMap = buyOrderMap.get(event.getPrice());
            orderMap.remove(event.getOrderId());
        } else {
            ConcurrentSkipListMap<Long, StockSellOrder> orderMap = sellOrderMap.get(event.getPrice());
            orderMap.remove(event.getOrderId());
        }
    }

    private void apply(StockSelledEvent event) {
        ConcurrentSkipListMap<Long, StockSellOrder> stockSellOrders = sellOrderMap.get(event.getPrice());
        if (event.isFinished()) {
            stockSellOrders.remove(event.getOrderId());
            if (stockSellOrders.isEmpty()) {
                sellOrderMap.remove(event.getPrice());
            }
        } else {
            StockSellOrder sellOrder = stockSellOrders.get(event.getOrderId());
            sellOrder.subtract(event.getTradingNumber());
        }
    }

    private void apply(StockBoughtEvent event) {
        ConcurrentSkipListMap<Long, StockBuyOrder> stockSellOrders = buyOrderMap.get(event.getPrice());
        if (event.isFinished()) {
            stockSellOrders.remove(event.getStockId());
            if (stockSellOrders.isEmpty()) {
                buyOrderMap.remove(event.getPrice());
            }
        } else {
            StockBuyOrder buyOrder = stockSellOrders.get(event.getOrderId());
            buyOrder.subtract(event.getTradingNumber());
        }
    }

    private void apply(StockBuyEntrustSucceedEvent event) {
        ConcurrentSkipListMap<Long, StockBuyOrder> stockSellOrders = buyOrderMap.computeIfAbsent(
                event.getBuyOrder().getPrice(), price -> new ConcurrentSkipListMap<>()
        );
        stockSellOrders.put(event.getBuyOrder().getOrderId(), event.getBuyOrder());
        tradeMap.put(event.getBuyOrder().getOrderId(), true);
    }

    private void apply(StockSelleEntrustSucceedEvent event) {
        ConcurrentSkipListMap<Long, StockSellOrder> stockSellOrders = sellOrderMap.computeIfAbsent(
                event.getSellOrder().getPrice(), price -> new ConcurrentSkipListMap<>()
        );
        stockSellOrders.put(event.getSellOrder().getOrderId(), event.getSellOrder());
        tradeMap.put(event.getSellOrder().getOrderId(), true);
    }
}
