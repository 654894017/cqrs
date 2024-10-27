package com.damon.cqrs.sample.trade_matching.domain.aggregate;

import com.damon.cqrs.domain.AggregateRoot;
import com.damon.cqrs.sample.trade_matching.api.cmd.*;
import com.damon.cqrs.sample.trade_matching.api.event.*;
import lombok.Getter;
import lombok.Setter;

import java.util.*;

@Getter
@Setter
public class Stock extends AggregateRoot {
    private final int maxDepth = 5;
    private Long realtimePrice = 100L;
    private Long notchPrice = 1L;
    private Map<Long, Boolean> tradeMap = new HashMap<>();
    private TreeMap<Long, TreeSet<StockBuyOrder>> buyOrderMap = new TreeMap<>(Comparator.reverseOrder());
    private TreeMap<Long, TreeSet<StockSellOrder>> sellOrderMap = new TreeMap<>();

    public Stock(Long id) {
        super(id);
    }

    /**
     * 市价单购买
     *
     * @param cmd
     * @return
     */
    public int buy(StockMarketBuyCmd cmd) {
        int remainingNumber = cmd.getNumber();
        NavigableMap<Long, TreeSet<StockSellOrder>> market5NotchMap = sellOrderMap.subMap(
                realtimePrice, true, realtimePrice + 5 * notchPrice, true
        );
        Set<MarketOrderBoughtEvent.TradeOrder> tradeOrders = new HashSet<>();
        MarketOrderBoughtEvent orderBoughtEvent = new MarketOrderBoughtEvent(
                cmd.getOrderId(), tradeOrders, cmd.getStockId(), cmd.getNumber(), cmd.getEntrustType()
        );
        for (TreeSet<StockSellOrder> sellOrders : market5NotchMap.values()) {
            for (StockSellOrder sellOrder : sellOrders) {
                int sellOrderNumber = sellOrder.getNumber();
                MarketOrderBoughtEvent.TradeOrder tradeOrder = new MarketOrderBoughtEvent.TradeOrder(
                        sellOrder.getOrderId(), sellOrderNumber <= remainingNumber,
                        Math.min(remainingNumber, sellOrderNumber), sellOrder.getPrice()
                );
                tradeOrders.add(tradeOrder);
                remainingNumber -= tradeOrder.getNumber();
                if (remainingNumber <= 0) {
                    applyNewEvent(orderBoughtEvent);
                    return 0;
                }
            }
        }
        applyNewEvent(orderBoughtEvent);
        return 0;
    }

    public int sell(StockMarketSellCmd cmd) {
        int remainingNumber = cmd.getNumber();
        NavigableMap<Long, TreeSet<StockBuyOrder>> market5NotchMap = buyOrderMap.subMap(
                realtimePrice, true, realtimePrice + 5 * notchPrice, true
        );
        Set<MarketOrderSelledEvent.TradeOrder> tradeOrders = new HashSet<>();
        MarketOrderSelledEvent orderSelledEvent = new MarketOrderSelledEvent(
                cmd.getOrderId(), tradeOrders, cmd.getStockId(), cmd.getNumber(), cmd.getEntrustType()
        );
        for (TreeSet<StockBuyOrder> buyOrders : market5NotchMap.values()) {
            for (StockBuyOrder buyOrder : buyOrders) {
                int buyOrderNumber = buyOrder.getNumber();
                MarketOrderSelledEvent.TradeOrder tradeOrder = new MarketOrderSelledEvent.TradeOrder(
                        buyOrder.getOrderId(), buyOrderNumber <= remainingNumber,
                        Math.min(remainingNumber, buyOrderNumber), buyOrder.getPrice()
                );
                tradeOrders.add(tradeOrder);
                remainingNumber -= tradeOrder.getNumber();
                if (remainingNumber <= 0) {
                    applyNewEvent(orderSelledEvent);
                    return 0;
                }
            }
        }
        applyNewEvent(orderSelledEvent);
        return 0;
    }

    public int sell(StockSellCmd cmd) {
        Boolean isTrade = tradeMap.get(cmd.getOrderId());
        if (isTrade == null) {
            applyNewEvent(new OrderSelleEntrustSucceedEvent(
                    new StockSellOrder(cmd.getPrice(), System.nanoTime(), cmd.getNumber(), cmd.getOrderId())
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
            applyNewEvent(new OrderCancelledEvent(cmd.getOrderId(), cmd.isBuyOrder() ? 1 : 0, cmd.getPrice()));
            return 0;
        }
    }

    public int buy(StockBuyCmd cmd) {
        Boolean isTrade = tradeMap.get(cmd.getOrderId());
        if (isTrade == null) {
            applyNewEvent(new OrderBuyEntrustSucceedEvent(
                    new StockBuyOrder(cmd.getPrice(), System.nanoTime(), cmd.getNumber(), cmd.getOrderId())
            ));
            return 0;
        } else {
            return -1;
        }
    }

    public int match(StockOrderMatchCmd cmd) {
        if (buyOrderMap.isEmpty()) {
            return -1;
        }
        StockBuyOrder buyOrder = buyOrderMap.firstEntry().getValue().first();
        if (buyOrder == null) {
            return -1;
        }
        if (sellOrderMap.isEmpty()) {
            return -1;
        }
        StockSellOrder sellOrder = sellOrderMap.firstEntry().getValue().first();
        if (sellOrder == null) {
            return -1;
        }
        if (buyOrder.getPrice() < sellOrder.getPrice()) {
            return -1;
        }
        boolean isSellDone = sellOrder.getNumber() <= buyOrder.getNumber();
        boolean isBuyDone = sellOrder.getNumber() >= buyOrder.getNumber();
        applyNewEvent(new OrderSelledEvent(sellOrder.getPrice(), sellOrder.getOrderId(),
                sellOrder.getNumber(), Math.min(buyOrder.getNumber() , sellOrder.getNumber()), isSellDone
        ));
        applyNewEvent(new OrderBoughtEvent(
                getId(), buyOrder.getPrice(), buyOrder.getOrderId(),
                buyOrder.getNumber(), Math.min(buyOrder.getNumber() , sellOrder.getNumber()), isBuyDone
        ));
        return 0;
    }

    private void apply(OrderCancelledEvent event) {
        if (event.getType() == 1) {
            TreeSet<StockBuyOrder> buyOrders = buyOrderMap.get(event.getPrice());
            buyOrders.remove(new StockBuyOrder(event.getOrderId()));
        } else {
            TreeSet<StockSellOrder> sellOrders = sellOrderMap.get(event.getPrice());
            sellOrders.remove(new StockSellOrder(event.getOrderId()));
        }
    }

    private void apply(OrderSelledEvent event) {
        TreeSet<StockSellOrder> stockSellOrders = sellOrderMap.get(event.getPrice());
        if (event.isDone()) {
            stockSellOrders.removeIf(stockSellOrder -> stockSellOrder.getOrderId().equals(event.getOrderId()));
        } else {
            StockSellOrder sellOrder = stockSellOrders.getFirst();
            sellOrder.subtract(event.getTradingNumber());
        }
    }

    private void apply(OrderBoughtEvent event) {
        TreeSet<StockBuyOrder> stockBuyOrders = buyOrderMap.get(event.getPrice());
        if (event.isDone()) {
            stockBuyOrders.removeIf(stockBuyOrder -> stockBuyOrder.getOrderId().equals(event.getOrderId()));
        } else {
            StockBuyOrder buyOrder = stockBuyOrders.getFirst();
            buyOrder.subtract(event.getTradingNumber());
        }
    }

    private void apply(OrderBuyEntrustSucceedEvent event) {
        TreeSet<StockBuyOrder> stockBuyOrders = buyOrderMap.computeIfAbsent(
                event.getBuyOrder().getPrice(), price -> new TreeSet<>(Comparator.comparing(StockBuyOrder::getCreateTime))
        );
        stockBuyOrders.add(event.getBuyOrder());
        tradeMap.put(event.getBuyOrder().getOrderId(), true);
    }

    private void apply(OrderSelleEntrustSucceedEvent event) {
        TreeSet<StockSellOrder> stockSellOrders = sellOrderMap.computeIfAbsent(
                event.getSellOrder().getPrice(), price -> new TreeSet<>(Comparator.comparing(StockSellOrder::getCreateTime))
        );
        stockSellOrders.add(event.getSellOrder());
        tradeMap.put(event.getSellOrder().getOrderId(), true);
    }
}
