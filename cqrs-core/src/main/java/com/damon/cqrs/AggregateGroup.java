package com.damon.cqrs;

import com.damon.cqrs.event.EventCommittingMailBox;
import lombok.Builder;
import lombok.Data;

/**
 * 聚合分组key
 *
 * @author xianping_lu
 */
@Data
@Builder
public class AggregateGroup {

    private long aggregateId;

    private String aggregateType;

    private EventCommittingMailBox eventCommittingMailBox;

    @Override
    public int hashCode() {
        return Long.hashCode(aggregateId);
    }

    @Override
    public boolean equals(Object obj) {

        if (!(obj instanceof AggregateGroup)) {
            return false;
        }

        AggregateGroup object = (AggregateGroup) obj;
        if (this == object) {
            return true;
        }

        return this.aggregateId == object.getAggregateId();

    }

}
