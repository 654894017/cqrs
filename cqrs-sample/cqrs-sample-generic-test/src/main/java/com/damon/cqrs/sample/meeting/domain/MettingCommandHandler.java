package com.damon.cqrs.sample.meeting.domain;

import com.damon.cqrs.CommandHandler;
import com.damon.cqrs.CqrsConfig;
import com.damon.cqrs.exception.AggregateEventConflictException;
import com.damon.cqrs.sample.meeting.api.IMettingCommandHandler;
import com.damon.cqrs.sample.meeting.api.command.*;
import com.damon.cqrs.sample.meeting.domain.aggregate.CancelReservationStatusEnum;
import com.damon.cqrs.sample.meeting.domain.aggregate.Meeting;
import com.damon.cqrs.sample.meeting.domain.aggregate.ReseveStatus;

public class MettingCommandHandler extends CommandHandler<Meeting> implements IMettingCommandHandler {

    public MettingCommandHandler(CqrsConfig cqrsConfig) {
        super(cqrsConfig);
    }

    @Override
    public void create(MettingCreateCommand create) {
        try {
            super.process(create, () -> new Meeting(create.getAggregateId(), create.getMeetingDate())).join();
        } catch (Exception e) {
            //如果是聚合根创建事件冲突异常，则说明聚合根已经被创建可以跳过
            if (!(e.getCause() instanceof AggregateEventConflictException)) {
                throw new RuntimeException(e);
            }
        }
    }

    @Override
    public ReseveStatus reserve(MettingReserveCommand reserve) {
        return super.process(reserve, meeting ->
                meeting.reserve(reserve)
        ).join();
    }

    @Override
    public CancelReservationStatusEnum cancel(MettingCancelCommand cancel) {
        return super.process(cancel, meeting ->
                meeting.cancel(cancel)
        ).join();
    }

    @Override
    public MettingDTO get(MettingGetCommand get) {
        return super.process(get, meeting ->
                new MettingDTO(meeting.getSchedule(), meeting.getMeetingDate(), meeting.getReserveRecord())
        ).join();
    }

}
