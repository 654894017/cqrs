package com.damon.cqrs.sample.meeting.api;

import com.damon.cqrs.sample.meeting.api.command.*;
import com.damon.cqrs.sample.meeting.domain.aggregate.CancelReservationStatusEnum;
import com.damon.cqrs.sample.meeting.domain.aggregate.ReseveStatus;

public interface IMettingCommandHandler {

    void create(MettingCreateCommand create);

    ReseveStatus reserve(MettingReserveCommand reserve);

    CancelReservationStatusEnum cancel(MettingCancelCommand cancel);

    MettingDTO get(MettingGetCommand get);

}
