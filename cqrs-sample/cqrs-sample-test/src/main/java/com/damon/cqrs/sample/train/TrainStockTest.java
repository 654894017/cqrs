package com.damon.cqrs.sample.train;


import java.util.BitSet;
import java.util.HashMap;
import java.util.Map;

/**
 * Created by 再青 .
 */
public class TrainStockTest {

    public static void main(String[] args) throws Exception {
        BitSet set = new BitSet(5);
        set.set(0);
        set.set(1);
        set.set(2);
        System.out.println(set);
        BitSet set2 = set.get(1, 55);
        System.out.println(set2.get(0));
        System.out.println(set2.get(1));
        System.out.println(set2.get(55555));

//        TrainStockTest test = new TrainStockTest();
//        test.testData();
//        test.testPerformance();
    }

    public void testData() throws Exception {
        Integer stationCount = 6;
        Integer seatCount = 10;
        TrainStock trainStock = buildTrainStock(stationCount, seatCount);
        System.out.println(trainStock.toString());
        //已出票索引,这么设计存储有性能问题,其实可以优化为List<Byte[]>,数据按位存储,实际应用中应该是和订单相关信息
        Map<String, TrainSeatMsg> bookedSeatMap = new HashMap<String, TrainSeatMsg>();
        //订票测试,并计入已出票
        long start = System.currentTimeMillis();
        for (int i = 1; i < 3; i++) {
            int num = trainStock.bookTicket(i, i + 1);
            if (num >= 0) {
                TrainSeatMsg trainSeatMsg = new TrainSeatMsg();
                trainSeatMsg.setFromStation(i);
                trainSeatMsg.setToStation(i + 1);
                trainSeatMsg.setSeatIndex(num);
                bookedSeatMap.put(i + "_" + (i + 1) + "_" + num, trainSeatMsg);
            }
            System.out.println((System.currentTimeMillis() - start) + "ms");
            System.out.println("Book s2s=" + i + "_" + (i + 1) + ":" + num);
            System.out.println(trainStock.toString());
        }
        //取消订票测试
        long start1 = System.currentTimeMillis();
        for (TrainSeatMsg value : bookedSeatMap.values()) {
            int num = trainStock.cancleTicket(value.fromStation, value.toStation, value.seatIndex);
            System.out.println((System.currentTimeMillis() - start1) + "ms");
            System.out.println("Cancle s2s=" + value.fromStation + "_" + value.toStation + "_" + value.seatIndex);
            System.out.println(trainStock.toString());
        }
        //余票查询
        long start2 = System.currentTimeMillis();
        for (int i = 1; i < stationCount; i++) {
            int num = trainStock.getStock(i, i + 1);
            System.out.println((System.currentTimeMillis() - start) + "ms");
            System.out.println("Get s2s=" + i + "_" + (i + 1) + ":" + num);
            System.out.println(trainStock.toString());
        }
    }


    public void testPerformance() throws Exception {
        Integer stationCount = 100;
        Integer seatCount = 2000;
        TrainStock trainStock = buildTrainStock(stationCount, seatCount);
        System.out.println(trainStock.toString());
        //订票测试,模拟stationCount*seatCount次,模拟出票情况最多的情况,即每次都购买相邻站并且每次都需要遍历,不存在被可购买索引挡出的情况
        Integer bookcount = 0;
        System.out.println("Book Ticket");
        long start = System.currentTimeMillis();
        for (int i = 1; i < stationCount; i++) {
            for (int j = 0; j < seatCount; j++) {
                int num = trainStock.bookTicket(i, i + 1);
                bookcount++;
            }
        }
        long bookTotalTime = System.currentTimeMillis() - start;
        System.out.println(bookTotalTime + "ms");
        System.out.println(bookcount + "times");
        System.out.println((float) bookTotalTime / (float) bookcount + "ms");
        System.out.println(bookcount / bookTotalTime * 1000 + "/s");
        //System.out.println(trainStock.toString());
        System.out.println("==========================");
        //余票查询,模拟stationCount*seatCount次,模拟最耗时的始发站到终点站的查询
        Integer getcount = 0;
        System.out.println("Get " + "Stock");
        long start1 = System.currentTimeMillis();
        for (int i = 1; i < stationCount; i++) {
            for (int j = 0; j < seatCount; j++) {
                int num = trainStock.getStock(1, stationCount);
                getcount++;
            }
        }
        long getTotalTime = System.currentTimeMillis() - start;
        System.out.println(getTotalTime + "ms");
        System.out.println(getcount + "times");
        System.out.println((float) getTotalTime / (float) getcount + "ms");
        System.out.println(getcount / getTotalTime * 1000 + "/s");
        //System.out.println(trainStock.toString());
    }

    /**
     * 构建车次库存数据实体
     *
     * @param stationCount 车站数量
     * @param seatCount    座位数量
     * @return
     */
    private TrainStock buildTrainStock(Integer stationCount, Integer seatCount) {
        TrainStock trainStock = new TrainStock();
        //构建车站map,车站索引从1开始构建
        Integer[] stations = new Integer[stationCount];
        for (int i = 0; i < stationCount; i++) {
            stations[i] = i + 1;
        }
        Map<Integer, Integer> stationMap = new HashMap<Integer, Integer>();
        for (int i = 0; i < stationCount; i++) {
            stationMap.put(stations[i], i);
        }
        //构建可预定判断索引
        BitSet canBookIndex = new BitSet(stationCount - 1);
        //构建车次购买限制Map,构建相邻车站最大可购买数量
        Map<Integer, Integer> s2sMaxCountMap = new HashMap<Integer, Integer>();
        for (int i = 0; i < stationCount - 1; i++) {
            s2sMaxCountMap.put(i * 10000 + i + 1, seatCount);
        }
        //构建相邻俩站剩余库存
        Integer[] s2stickets = new Integer[stationCount - 1];
        for (int i = 0; i < stationCount - 1; i++) {
            s2stickets[i] = seatCount;

        }
        //构建座位可预定判断索引Map
        BitSet[] seatCanBookIndexs = new BitSet[seatCount];
        for (int i = 0; i < seatCanBookIndexs.length; i++) {
            seatCanBookIndexs[i] = new BitSet(stationCount - 1);
        }
        //构建库存数据实体类
        trainStock.stationMap = stationMap;
        trainStock.canBookIndex = canBookIndex;
        trainStock.s2sMaxCountMap = s2sMaxCountMap;
        trainStock.s2sTickets = s2stickets;
        trainStock.seatCanBookIndex = seatCanBookIndexs;
        return trainStock;
    }

    /**
     * 车次库存
     */
    public class TrainStock {
        public Map<Integer, Integer> stationMap = new HashMap<Integer, Integer>();
        public BitSet canBookIndex;
        public Map<Integer, Integer> s2sMaxCountMap = new HashMap<Integer, Integer>();
        public Integer[] s2sTickets;
        public BitSet[] seatCanBookIndex;

        @Override
        public String toString() {
            String temp = "stationMap:" + stationMap.toString() + "\r\n";
            temp = temp + "s2sTickets:";
            for (int i = 0; i < s2sTickets.length; i++) {
                temp = temp + "," + s2sTickets[i];
            }
            temp = temp + "\r\n";
            temp = temp + "canBookIndex:" + canBookIndex.toString() + "\r\n";

            temp = temp + "s2sMaxCountMap:" + s2sMaxCountMap.toString() + "\r\n";

            temp = temp + "seatCanBookIndex:";
            for (int i = 0; i < seatCanBookIndex.length; i++) {
                temp = temp + "," + seatCanBookIndex[i].toString();
            }
            temp = temp + "\r\n";
            return temp;
        }

        public Integer bookTicket(Integer fromStation, Integer toStation) {
            Integer fIdx = stationMap.get(fromStation);
            Integer tIdx = stationMap.get(toStation);
            BitSet bookIndex = this.canBookIndex.get(fIdx, tIdx);
            if (bookIndex.isEmpty()) {
                Integer maxCount = s2sMaxCountMap.get(fromStation * 10000 + toStation);
                if (maxCount != null && maxCount > 0 || maxCount == null) {
                    for (int i = 0; i < this.seatCanBookIndex.length; i++) {
                        BitSet seatBookIndex = this.seatCanBookIndex[i].get(fIdx, tIdx);
                        if (seatBookIndex.isEmpty()) {
                            for (int j = fIdx; j < tIdx; j++) {
                                int temp1 = s2sTickets[j] - 1;
                                s2sTickets[j] = temp1;
                                if (temp1 == 0)
                                    canBookIndex.set(j);
                                this.seatCanBookIndex[i].set(j);
                            }
                            if (maxCount != null)
                                this.s2sMaxCountMap.put(fromStation * 10000 + toStation, maxCount - 1);
                            return i;
                        }
                    }
                    return -101;
                } else
                    return -102;
            } else {
                return -100;
            }
        }

        public Integer cancleTicket(Integer fromStation, Integer toStation, int seatIndex) {
            Integer fIdx = stationMap.get(fromStation);
            Integer tIdx = stationMap.get(toStation);
            for (int j = fIdx; j < tIdx; j++) {
                int temp1 = s2sTickets[j] + 1;
                s2sTickets[j] = temp1;
                if (temp1 == 0)
                    canBookIndex.set(j);
                this.seatCanBookIndex[seatIndex].set(j, false);
            }
            Integer maxCount = this.s2sMaxCountMap.get(fromStation * 10000 + toStation);
            if (maxCount != null)
                this.s2sMaxCountMap.put(fromStation * 10000 + toStation, maxCount + 1);
            return 1;
        }

        public Integer getStock(Integer fromStation, Integer toStation) {
            Integer fIdx = this.stationMap.get(fromStation);
            Integer tIdx = this.stationMap.get(toStation);
            int temp1 = 10000;
            for (int j = fIdx; j < tIdx; j++) {
                if (temp1 > s2sTickets[j])
                    temp1 = s2sTickets[j];
            }
            return temp1;
        }
    }

    /**
     * 车次坐席信息
     */
    public class TrainSeatMsg {
        Integer fromStation;
        Integer toStation;
        Integer seatIndex;

        public Integer getSeatIndex() {
            return seatIndex;
        }

        public void setSeatIndex(Integer seatIndex) {
            this.seatIndex = seatIndex;
        }

        public Integer getFromStation() {
            return fromStation;
        }

        public void setFromStation(Integer fromStation) {
            this.fromStation = fromStation;
        }

        public Integer getToStation() {
            return toStation;
        }

        public void setToStation(Integer toStation) {
            this.toStation = toStation;
        }
    }

}

