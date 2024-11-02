package com.damon.cqrs.sample.trade_matching.domain.aggregate;

import com.damon.cqrs.domain.AggregateRoot;
import com.damon.cqrs.sample.trade_matching.domain.cmd.*;
import com.damon.cqrs.sample.trade_matching.domain.event.*;
import lombok.Getter;
import lombok.Setter;

import java.util.*;

@Getter
@Setter
public class Stock extends AggregateRoot {
    /**
     * 股票实时价格
     */
    private Long realtimePrice;
    /**
     * 一个档位的价格
     */
    private Long notchPrice;
    private Map<Long, Boolean> tradeMap = new HashMap<>();
    /**
     * 先按价格档位降序, 在同档位内按下单时间升序, 类型: <price,<orderId, order>>
     */
    private TreeMap<Long, TreeMap<Long, StockBuyOrder>> buyOrderMap = new TreeMap<>(Comparator.reverseOrder());
    /**
     * 先按价格档位升序, 在同档位内按下单时间升序, 类型: <price,<orderId, order>>
     */
    private TreeMap<Long, TreeMap<Long, StockSellOrder>> sellOrderMap = new TreeMap<>();

    public Stock(Long id) {
        super(id);
    }

    /**
     * 集合竞价(开盘价/收盘价都是通过此方法)
     * <p>
     * https://m.gelonghui.com/p/513097
     *
     * @return
     */
    public int callAuction(CallAuctionCmd cmd) {
        Map<Long, Long> sellPriceMap = new HashMap<>();
        sellOrderMap.keySet().forEach(price -> {
            Long totalNumber = sellOrderMap.tailMap(price).values().stream().flatMap(treeMap -> treeMap.values().stream())
                    .mapToLong(StockSellOrder::getNumber).sum();
            sellPriceMap.put(price, totalNumber);
        });

        Map<Long, Long> buyPriceMap = new HashMap<>();
        buyOrderMap.keySet().forEach(price -> {
            Long totalNumber = buyOrderMap.tailMap(price).values().stream().flatMap(treeMap -> treeMap.values().stream())
                    .mapToLong(StockBuyOrder::getNumber).sum();
            buyPriceMap.put(price, totalNumber);
        });

        TreeMap<Long, Long> maxTradeMap = new TreeMap<>(Comparator.reverseOrder());
        sellPriceMap.forEach((price, totalNumber) -> {
            Long maxTradeNumber = Math.min(buyPriceMap.get(price), totalNumber);
            maxTradeMap.put(maxTradeNumber, price);
        });
        Map.Entry<Long, Long> entry = maxTradeMap.firstEntry();
        if (!maxTradeMap.isEmpty() && entry.getValue() != null) {
            applyNewEvent(new CallAuctionSucceedEvent(entry.getValue()));
        }
        return 0;
    }

    /**
     * 市价单购买
     *
     * @param cmd
     * @return
     */
    public int buy(StockMarketBuyCmd cmd) {
        Integer remainingNumber = cmd.getNumber();
        NavigableMap<Long, TreeMap<Long, StockSellOrder>> market5NotchMap = sellOrderMap.subMap(
                realtimePrice, true, realtimePrice + 5 * notchPrice, true
        );
        LinkedHashSet<MarketOrderBoughtEvent.TradeOrder> tradeOrders = new LinkedHashSet<>();
        MarketOrderBoughtEvent orderBoughtEvent = new MarketOrderBoughtEvent(
                cmd.getOrderId(), tradeOrders, cmd.getStockId(), cmd.getNumber(), cmd.getEntrustmentType()
        );
        for (TreeMap<Long, StockSellOrder> sellOrders : market5NotchMap.values()) {
            for (StockSellOrder sellOrder : sellOrders.values()) {
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
        if (tradeOrders.isEmpty()) {
            return -1;
        }
        applyNewEvent(orderBoughtEvent);
        return 0;
    }

    /**
     * 市价单售卖
     *
     * @param cmd
     * @return
     */
    public int sell(StockMarketSellCmd cmd) {
        Integer remainingNumber = cmd.getNumber();
        NavigableMap<Long, TreeMap<Long, StockBuyOrder>> market5NotchMap = buyOrderMap.subMap(
                realtimePrice + 5 * notchPrice, true, realtimePrice, true
        );
        LinkedHashSet<MarketOrderSelledEvent.TradeOrder> tradeOrders = new LinkedHashSet<>();
        MarketOrderSelledEvent orderSelledEvent = new MarketOrderSelledEvent(
                cmd.getOrderId(), tradeOrders, cmd.getStockId(), cmd.getNumber(), cmd.getEntrustmentType()
        );

        for (TreeMap<Long, StockBuyOrder> buyOrders : market5NotchMap.values()) {
            for (StockBuyOrder buyOrder : buyOrders.values()) {
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
        if (tradeOrders.isEmpty()) {
            return -1;
        }
        applyNewEvent(orderSelledEvent);
        return 0;
    }

    /**
     * 限价卖单
     *
     * @param cmd
     * @return
     */
    public int sell(StockSellCmd cmd) {
        Boolean isTrade = tradeMap.get(cmd.getOrderId());
        if (isTrade == null) {
            applyNewEvent(new OrderSelleEntrustSucceedEvent(cmd.getOrderId(), cmd.getPrice(), System.nanoTime(), cmd.getNumber()));
            return 0;
        } else {
            return -1;
        }
    }

    /**
     * 取消委托
     *
     * @param cmd
     * @return
     */
    public int cancel(StockOrderCancelCmd cmd) {
        Boolean isTrade = tradeMap.get(cmd.getOrderId());
        if (isTrade == null) {
            return -1;
        } else {
            applyNewEvent(new OrderCancelledEvent(cmd.getOrderId(), cmd.isBuyOrder() ? 1 : 0, cmd.getPrice()));
            return 0;
        }
    }

    /**
     * 限价买单
     *
     * @param cmd
     * @return
     */
    public int buy(StockBuyCmd cmd) {
        Boolean isTrade = tradeMap.get(cmd.getOrderId());
        if (isTrade == null) {
            applyNewEvent(new OrderBuyEntrustSucceedEvent(cmd.getOrderId(), cmd.getPrice(), System.nanoTime(), cmd.getNumber()));
            return 0;
        } else {
            return -1;
        }
    }

    /**
     * 撮合交易
     *
     * @param cmd
     * @return
     */
    public int match(StockOrderMatchCmd cmd) {
        if (buyOrderMap.isEmpty()) {
            return -1;
        }
        TreeMap<Long, StockBuyOrder> buyOrders = buyOrderMap.firstEntry().getValue();
        if (buyOrders.isEmpty()) {
            return -1;
        }
        Map.Entry<Long, StockBuyOrder> buyOrderEntry = buyOrders.firstEntry();
        if (buyOrderEntry == null) {
            return -1;
        }
        StockBuyOrder buyOrder = buyOrderEntry.getValue();
        if (buyOrder == null) {
            return -1;
        }
        if (sellOrderMap.isEmpty()) {
            return -1;
        }
        TreeMap<Long, StockSellOrder> sellOrders = sellOrderMap.firstEntry().getValue();
        if (sellOrders.isEmpty()) {
            return -1;
        }
        Map.Entry<Long, StockSellOrder> sellOrderEntry = sellOrders.firstEntry();
        if (sellOrderEntry == null) {
            return -1;
        }
        StockSellOrder sellOrder = sellOrderEntry.getValue();
        if (sellOrder == null) {
            return -1;
        }
        if (buyOrder.getPrice() < sellOrder.getPrice()) {
            return -1;
        }
        boolean isSellDone = sellOrder.getNumber() <= buyOrder.getNumber();
        boolean isBuyDone = sellOrder.getNumber() >= buyOrder.getNumber();
        applyNewEvent(new OrderSelledEvent(sellOrder.getPrice(), sellOrder.getOrderId(),
                sellOrder.getOriginalNumber(), Math.min(buyOrder.getNumber(), sellOrder.getNumber()), isSellDone
        ));
        applyNewEvent(new OrderBoughtEvent(
                getId(), buyOrder.getPrice(), sellOrder.getPrice(), buyOrder.getOrderId(),
                sellOrder.getOriginalNumber(), Math.min(buyOrder.getNumber(), sellOrder.getNumber()), isBuyDone
        ));
        return 0;
    }

    private void apply(MarketOrderBoughtEvent event) {
        event.getTradeOrders().forEach(tradeOrder -> {
            TreeMap<Long, StockSellOrder> priceSellOrders = sellOrderMap.get(tradeOrder.getPrice());
            if (tradeOrder.isDone()) {
                priceSellOrders.remove(tradeOrder.getSellerOrderId());
            } else {
                StockSellOrder sellOrder = priceSellOrders.get(tradeOrder.getSellerOrderId());
                sellOrder.subtract(tradeOrder.getNumber());
            }
        });
        if (event.isUndone() && event.isTransferLimitOrderEntrustment()) {
            TreeMap<Long, StockBuyOrder> stockBuyOrders = buyOrderMap.computeIfAbsent(
                    realtimePrice, price -> new TreeMap<>()
            );
            StockBuyOrder buyOrder = new StockBuyOrder(event.getOrderId(), realtimePrice, System.nanoTime(), event.undoneNumber());
            stockBuyOrders.put(event.getOrderId(), buyOrder);
            tradeMap.put(event.getOrderId(), true);
        }

        if (!event.getTradeOrders().isEmpty()) {
            MarketOrderBoughtEvent.TradeOrder tradeOrder = event.getTradeOrders().getLast();
            this.realtimePrice = tradeOrder.getPrice();
        }
    }

    private void apply(MarketOrderSelledEvent event) {
        event.getTradeOrders().forEach(tradeOrder -> {
            TreeMap<Long, StockBuyOrder> priceSellOrders = buyOrderMap.get(tradeOrder.getPrice());
            if (tradeOrder.isDone()) {
                priceSellOrders.remove(tradeOrder.getBuyerOrderId());
            } else {
                StockBuyOrder buyOrder = priceSellOrders.get(tradeOrder.getBuyerOrderId());
                buyOrder.subtract(tradeOrder.getNumber());
            }
        });

        if (event.isUndone() && event.isTransferLimitOrderEntrustment()) {
            TreeMap<Long, StockSellOrder> stockSellOrders = sellOrderMap.computeIfAbsent(
                    realtimePrice, price -> new TreeMap<>()
            );
            StockSellOrder sellOrder = new StockSellOrder(event.getOrderId(), realtimePrice, event.undoneNumber(), System.nanoTime());
            stockSellOrders.put(event.getOrderId(), sellOrder);
            tradeMap.put(event.getOrderId(), true);
        }

        if (!event.getTradeOrders().isEmpty()) {
            MarketOrderSelledEvent.TradeOrder tradeOrder = event.getTradeOrders().getLast();
            this.realtimePrice = tradeOrder.getPrice();
        }
    }

    private void apply(OrderCancelledEvent event) {
        if (event.isBuyOrder()) {
            TreeMap<Long, StockBuyOrder> buyOrders = buyOrderMap.get(event.getPrice());
            buyOrders.remove(event.getOrderId());
        } else {
            TreeMap<Long, StockSellOrder> sellOrders = sellOrderMap.get(event.getPrice());
            sellOrders.remove(event.getOrderId());
        }
    }

    private void apply(OrderSelledEvent event) {
        TreeMap<Long, StockSellOrder> stockSellOrders = sellOrderMap.get(event.getPrice());
        if (event.isDone()) {
            stockSellOrders.remove(event.getOrderId());
        } else {
            StockSellOrder sellOrder = stockSellOrders.get(event.getOrderId());
            sellOrder.subtract(event.getTradingNumber());
        }
        this.realtimePrice = event.getPrice();
    }

    private void apply(OrderBoughtEvent event) {
        TreeMap<Long, StockBuyOrder> stockBuyOrders = buyOrderMap.get(event.getEntrustPrice());
        if (event.isDone()) {
            stockBuyOrders.remove(event.getOrderId());
        } else {
            StockBuyOrder buyOrder = stockBuyOrders.get(event.getOrderId());
            buyOrder.subtract(event.getTradingNumber());
        }
        this.realtimePrice = event.getBuyPrice();
    }

    private void apply(OrderBuyEntrustSucceedEvent event) {
        TreeMap<Long, StockBuyOrder> stockBuyOrders = buyOrderMap.computeIfAbsent(
                event.getPrice(), price -> new TreeMap<>()
        );
        StockBuyOrder buyOrder = new StockBuyOrder(event.getOrderId(), event.getPrice(), event.getCreateTime(), event.getNumber());
        stockBuyOrders.put(buyOrder.getOrderId(), buyOrder);
        tradeMap.put(buyOrder.getOrderId(), true);
    }

    private void apply(OrderSelleEntrustSucceedEvent event) {
        TreeMap<Long, StockSellOrder> stockSellOrders = sellOrderMap.computeIfAbsent(
                event.getPrice(), price -> new TreeMap<>()
        );
        StockSellOrder sellOrder = new StockSellOrder(event.getOrderId(), event.getPrice(), event.getNumber(), event.getCreateTime());
        stockSellOrders.put(sellOrder.getOrderId(), sellOrder);
        tradeMap.put(sellOrder.getOrderId(), true);
    }

    private void apply(CallAuctionSucceedEvent event) {
        this.realtimePrice = event.getPrice();
    }
}
