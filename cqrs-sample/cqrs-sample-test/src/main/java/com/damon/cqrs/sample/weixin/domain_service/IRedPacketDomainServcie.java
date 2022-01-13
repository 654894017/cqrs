package com.damon.cqrs.sample.weixin.domain_service;

import com.damon.cqrs.sample.weixin.dto.WeixinRedPacketDTO;
import com.damon.cqrs.sample.weixin.command.RedPacketCreateCommand;
import com.damon.cqrs.sample.weixin.command.RedPacketGetCommand;
import com.damon.cqrs.sample.weixin.command.RedPacketGrabCommand;

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

