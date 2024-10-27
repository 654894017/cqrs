package com.damon.cqrs.sample.metting.domain;

import com.damon.cqrs.command.CommandService;
import com.damon.cqrs.config.CqrsConfig;
import com.damon.cqrs.sample.metting.api.IMettingCommandService;
import com.damon.cqrs.sample.metting.api.command.MettingCancelCommand;
import com.damon.cqrs.sample.metting.api.command.MettingDTO;
import com.damon.cqrs.sample.metting.api.command.MettingGetCommand;
import com.damon.cqrs.sample.metting.api.command.MettingReserveCommand;
import com.damon.cqrs.sample.metting.domain.aggregate.CancelReservationStatusEnum;
import com.damon.cqrs.sample.metting.domain.aggregate.MeetingId;
import com.damon.cqrs.sample.metting.domain.aggregate.Metting;
import com.damon.cqrs.sample.metting.domain.aggregate.ReseveStatus;
import lombok.extern.slf4j.Slf4j;

@Slf4j
public class MettingCommandService extends CommandService<Metting> implements IMettingCommandService {

    public MettingCommandService(CqrsConfig cqrsConfig) {
        super(cqrsConfig);
    }

    @Override
    public ReseveStatus reserve(MettingReserveCommand command) {
        return super.process(command, metting -> metting.reserve(command)).join();
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

    @Override
    public Metting getAggregateSnapshot(long aggregateId, Class<Metting> classes) {
        Metting metting = new Metting(new MeetingId(aggregateId));
        return metting;
    }
}
