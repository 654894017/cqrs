package com.damon.cqrs.sample.red_packet.domain_service;

import com.damon.cqrs.sample.red_packet.command.RedPacketCreateCommand;
import com.damon.cqrs.sample.red_packet.command.RedPacketGetCommand;
import com.damon.cqrs.sample.red_packet.command.RedPacketGrabCommand;
import com.damon.cqrs.sample.red_packet.dto.WeixinRedPacketDTO;

public interface IRedPacketDomainServcie {
    /**
     * 创建红包
     *
     * @param command
     */
    void createRedPackage(RedPacketCreateCommand command);

    /**
     * 抢红包
     *
     * @param command
     * @return
     */
    int grabRedPackage(RedPacketGrabCommand command);

    /**
     * 红包详情
     *
     * @param command
     * @return
     */
    WeixinRedPacketDTO get(RedPacketGetCommand command);
}

