package com.damon.cqrs.sample.meeting.api;

import com.damon.cqrs.sample.meeting.api.command.MettingCancelCommand;
import com.damon.cqrs.sample.meeting.api.command.MettingDTO;
import com.damon.cqrs.sample.meeting.api.command.MettingGetCommand;
import com.damon.cqrs.sample.meeting.api.command.MettingReserveCommand;
import com.damon.cqrs.sample.meeting.domain.aggregate.CancelReservationStatusEnum;
import com.damon.cqrs.sample.meeting.domain.aggregate.ReseveStatus;

public interface IMettingCommandHandler {

    ReseveStatus reserve(MettingReserveCommand reserve);

    CancelReservationStatusEnum cancel(MettingCancelCommand cancel);

    MettingDTO get(MettingGetCommand get);

}
