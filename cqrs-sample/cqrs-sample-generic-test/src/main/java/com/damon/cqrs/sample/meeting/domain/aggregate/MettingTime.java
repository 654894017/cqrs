package com.damon.cqrs.sample.meeting.domain.aggregate;

import com.damon.cqrs.domain.ValueObject;
import lombok.NonNull;


public class MettingTime extends ValueObject {
    private Integer start;
    private Integer end;

    public MettingTime(@NonNull Integer start, @NonNull Integer end) {
        if (start >= end) {
            throw new IllegalArgumentException("会议时间范围无效");
        }
        if (start < 0 || start > 1440) {
            throw new IllegalArgumentException("会议开始时间无效");
        }
        if (end < 1 || end > 1440) {
            throw new IllegalArgumentException("会议结束时间无效");
        }
        this.start = start;
        this.end = end;
    }

    public Integer getStart() {
        return start;
    }

    public Integer getEnd() {
        return end;
    }

}
