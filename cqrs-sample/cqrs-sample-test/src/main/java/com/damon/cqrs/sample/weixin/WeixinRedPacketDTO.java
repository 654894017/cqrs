package com.damon.cqrs.sample.weixin;

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
    private Map<Long, Long> map;

    private Stack<Long> redpacketStack;

    private RedPacketTypeEnum type;

    private Long sponsorId;

    private Long id;

}
