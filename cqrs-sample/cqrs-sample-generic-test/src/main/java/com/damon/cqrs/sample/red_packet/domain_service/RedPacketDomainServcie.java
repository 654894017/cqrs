package com.damon.cqrs.sample.red_packet.domain_service;

import com.damon.cqrs.AbstractDomainService;
import com.damon.cqrs.event.EventCommittingService;
import com.damon.cqrs.sample.red_packet.aggregate.WeixinRedPacket;
import com.damon.cqrs.sample.red_packet.command.RedPacketCreateCommand;
import com.damon.cqrs.sample.red_packet.command.RedPacketGetCommand;
import com.damon.cqrs.sample.red_packet.command.RedPacketGrabCommand;
import com.damon.cqrs.sample.red_packet.dto.WeixinRedPacketDTO;

import java.util.concurrent.CompletableFuture;

/**
 * @author xianpinglu
 */
public class RedPacketDomainServcie extends AbstractDomainService<WeixinRedPacket> implements IRedPacketDomainServcie {

    public RedPacketDomainServcie(EventCommittingService eventCommittingService) {
        super(eventCommittingService);
    }

    @Override
    public void createRedPackage(RedPacketCreateCommand command) {
        super.process(command, () ->
                new WeixinRedPacket(
                        command.getAggregateId(),
                        command.getMoney(),
                        command.getNumber(),
                        command.getType(),
                        command.getSponsorId()
                )
        ).join();
        return;
    }

    @Override
    public int grabRedPackage(final RedPacketGrabCommand command) {
        CompletableFuture<Integer> future = super.process(
                command,
                redPacket -> redPacket.grabRedPackage(command.getUserId())
        );

        return future.join();
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
                    redPacketDTO.setType(redPacket.getType());
                    return redPacketDTO;
                }
        );
        return future.join();
    }

    @Override
    public CompletableFuture<WeixinRedPacket> getAggregateSnapshoot(long aggregateId, Class<WeixinRedPacket> classes) {
        return CompletableFuture.completedFuture(null);
    }

    @Override
    public CompletableFuture<Boolean> saveAggregateSnapshoot(WeixinRedPacket aggregate) {
        return CompletableFuture.completedFuture(true);
    }

}
