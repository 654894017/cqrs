package com.damon.cqrs.sample.meeting.domain;

import com.damon.cqrs.CommandService;
import com.damon.cqrs.CqrsConfig;
import com.damon.cqrs.sample.meeting.api.IMettingCommandService;
import com.damon.cqrs.sample.meeting.api.command.MettingCancelCommand;
import com.damon.cqrs.sample.meeting.api.command.MettingDTO;
import com.damon.cqrs.sample.meeting.api.command.MettingGetCommand;
import com.damon.cqrs.sample.meeting.api.command.MettingReserveCommand;
import com.damon.cqrs.sample.meeting.domain.aggregate.CancelReservationStatusEnum;
import com.damon.cqrs.sample.meeting.domain.aggregate.Metting;
import com.damon.cqrs.sample.meeting.domain.aggregate.ReseveStatus;
import lombok.extern.slf4j.Slf4j;

@Slf4j
public class MettingCommandService extends CommandService<Metting> implements IMettingCommandService {

    public MettingCommandService(CqrsConfig cqrsConfig) {
        super(cqrsConfig);
    }

    @Override
    public ReseveStatus reserve(MettingReserveCommand command) {
        return super.process(command,
                () -> new Metting(command.getAggregateId(), command.getMettingDate()),
                metting -> metting.reserve(command)
        ).join();
    }

    @Override
    public CancelReservationStatusEnum cancel(MettingCancelCommand cancel) {
        return super.process(cancel, metting ->
                metting.cancel(cancel)
        ).join();
    }

    @Override
    public MettingDTO get(MettingGetCommand get) {
        return super.process(get, metting ->
                new MettingDTO(metting.getSchedule(), metting.getMeetingDate(), metting.getReserveRecord())
        ).join();
    }

}
