package com.damon.cqrs.sample.red_packet.api.dto;

import lombok.Data;

import java.util.Map;
import java.util.Stack;

/**
 * @author xianpinglu
 */
@Data
public class WeixinRedPacketDTO {
    /**
     * key 用户id   value  抢到的金额
     */
    private Map<Long, Double> map;

    private Stack<Double> redpacketStack;

    private Long sponsorId;

    private Long id;

}
