package com.damon.cqrs.sample.meeting.domain.aggregate;

import com.damon.cqrs.domain.AggregateRoot;
import com.damon.cqrs.sample.meeting.api.command.MettingCancelCommand;
import com.damon.cqrs.sample.meeting.api.command.MettingReserveCommand;
import com.damon.cqrs.sample.meeting.api.event.MettingCancelledEvent;
import com.damon.cqrs.sample.meeting.api.event.MettingCreatedEvent;
import com.damon.cqrs.sample.meeting.api.event.MettingReservedEvent;
import lombok.Getter;
import lombok.NonNull;
import lombok.ToString;

import java.util.BitSet;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

/**
 * 会议室聚合根
 */
@Getter
@ToString
public class Meeting extends AggregateRoot {
    /**
     * 会议室预定情况
     */
    private BitSet schedule;
    /**
     * 会议室日期
     */
    private String meetingDate;
    /**
     * 会议室预定记录
     */
    private Map<String, ReserveInfo> reserveRecord;

    public Meeting() {
    }

    public Meeting(@NonNull Long aggregateId, @NonNull String meetingDate) {
        super(aggregateId);
        super.applyNewEvent(new MettingCreatedEvent(meetingDate));
    }

    /**
     * 预定
     *
     * @param command
     * @return 0 成功 -1 存在被占用
     */
    public ReseveStatus reserve(@NonNull MettingReserveCommand command) {
        MettingTime mettingTime = command.getMettingTime();
        BitSet set = schedule.get(mettingTime.getStart(), mettingTime.getEnd());
        if (set.cardinality() > 0) {
            //区间已被占用
            return new ReseveStatus(ReserveStatusEnum.BEOCCUPIED);
        }
        String reserveFlag = UUID.randomUUID().toString();
        super.applyNewEvent(new MettingReservedEvent(
                mettingTime.getStart(),
                mettingTime.getEnd(),
                command.getUserId(),
                reserveFlag,
                command.getMettingTopic(),
                command.getMettingContent(),
                command.getAttachmentUrl()
        ));
        return new ReseveStatus(ReserveStatusEnum.SUCCEEDED, reserveFlag);
    }

    /**
     * 取消
     *
     * @param command
     * @return 0 成功  -1 不存在预定  -2 当前用户不存在预定记录
     */
    public CancelReservationStatusEnum cancel(@NonNull MettingCancelCommand command) {
        ReserveInfo reserveInfo = reserveRecord.get(command.getReserveFlag());
        if (reserveInfo == null) {
            return CancelReservationStatusEnum.NONEXISTENT;
        }

        if (!reserveInfo.getUserId().equals(command.getUserId())) {
            return CancelReservationStatusEnum.UNMACHED;
        }

        super.applyNewEvent(new MettingCancelledEvent(command.getReserveFlag(), reserveInfo.getStart(), reserveInfo.getEnd()));
        return CancelReservationStatusEnum.SUCCEEDED;
    }


    private void apply(MettingCreatedEvent event) {
        this.schedule = new BitSet(1440);
        this.meetingDate = event.getMeetingDate();
        this.reserveRecord = new HashMap<>();
    }


    private void apply(MettingReservedEvent event) {
        schedule.set(event.getStart(), event.getEnd());
        reserveRecord.put(event.getReserveFlag(),
                new ReserveInfo(event.getUserId(),
                        event.getStart(),
                        event.getEnd(),
                        event.getMettingTopic(),
                        event.getMettingContent(),
                        event.getAttachmentUrl()
                )
        );
    }


    private void apply(MettingCancelledEvent event) {
        schedule.clear(event.getStart(), event.getEnd());
        reserveRecord.remove(event.getReserveFlag());
    }

}
