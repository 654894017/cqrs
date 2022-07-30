package com.damon.cqrs.sample.red_packet.domain.service;

import com.damon.cqrs.AbstractCommandHandler;
import com.damon.cqrs.CQRSConfig;
import com.damon.cqrs.sample.red_packet.api.IRedPacketCommandHandler;
import com.damon.cqrs.sample.red_packet.api.command.RedPacketCreateCommand;
import com.damon.cqrs.sample.red_packet.api.command.RedPacketGetCommand;
import com.damon.cqrs.sample.red_packet.api.command.RedPacketGrabCommand;
import com.damon.cqrs.sample.red_packet.api.dto.WeixinRedPacketDTO;
import com.damon.cqrs.sample.red_packet.domain.aggregate.WeixinRedPacket;

import java.util.concurrent.CompletableFuture;

/**
 * @author xianpinglu
 */
public class RedPacketCommandHandler extends AbstractCommandHandler<WeixinRedPacket> implements IRedPacketCommandHandler {

    public RedPacketCommandHandler(CQRSConfig config) {
        super(config);
    }

    @Override
    public void createRedPackage(RedPacketCreateCommand command) {
        super.process(command, () -> new WeixinRedPacket(command)).join();
        return;
    }

    @Override

    public int grabRedPackage(final RedPacketGrabCommand command) {
        return super.process(command, redPacket -> redPacket.grabRedPackage(command)).join();
    }

    @Override
    public WeixinRedPacketDTO get(RedPacketGetCommand command) {
        CompletableFuture<WeixinRedPacketDTO> future = super.process(
                command,
                redPacket -> {
                    WeixinRedPacketDTO redPacketDTO = new WeixinRedPacketDTO();
                    redPacketDTO.setMap(redPacket.getMap());
                    redPacketDTO.setId(redPacket.getId());
                    redPacketDTO.setRedpacketStack(redPacket.getRedpacketStack());
                    redPacketDTO.setSponsorId(redPacket.getSponsorId());
                    return redPacketDTO;
                }
        );
        return future.join();
    }


}
