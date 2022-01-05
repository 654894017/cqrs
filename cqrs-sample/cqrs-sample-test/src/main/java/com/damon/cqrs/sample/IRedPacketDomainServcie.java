package com.damon.cqrs.sample;

public interface IRedPacketDomainServcie {

    void createRedPackage(RedPacketCreateCommand command);
    /**
     * 抢红包
     *
     * @param command
     * @return
     */
    int grabRedPackage(RedPacketGrabCommand command);
}

