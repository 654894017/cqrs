package com.damon.cqrs.domain;

public interface IAggregateRootBaseSetting {

    /**
     * 创建聚合根快照周期(单位秒，小于0不创建).
     * 注意：聚合的快照不一定是当前最新的版本。
     *
     * @return
     */
    default long createSnapshotCycle() {
        return -1;
    }
}
