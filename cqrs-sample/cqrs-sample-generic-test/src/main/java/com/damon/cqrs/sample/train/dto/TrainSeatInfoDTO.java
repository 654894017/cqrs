package com.damon.cqrs.sample.train.dto;

/**
 * 车次坐席信息
 */
public class TrainSeatInfoDTO {
    private Integer startStation;
    private Integer endStation;
    private Integer seatIndex;

    public Integer getStartStation() {
        return startStation;
    }

    public void setStartStation(Integer startStation) {
        this.startStation = startStation;
    }

    public Integer getEndStation() {
        return endStation;
    }

    public void setEndStation(Integer endStation) {
        this.endStation = endStation;
    }

    public Integer getSeatIndex() {
        return seatIndex;
    }

    public void setSeatIndex(Integer seatIndex) {
        this.seatIndex = seatIndex;
    }
}