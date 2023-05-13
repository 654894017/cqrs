package com.damon.cqrs.sample.metting.api;

import com.damon.cqrs.sample.metting.api.command.MettingCancelCommand;
import com.damon.cqrs.sample.metting.api.command.MettingDTO;
import com.damon.cqrs.sample.metting.api.command.MettingGetCommand;
import com.damon.cqrs.sample.metting.api.command.MettingReserveCommand;
import com.damon.cqrs.sample.metting.domain.aggregate.CancelReservationStatusEnum;
import com.damon.cqrs.sample.metting.domain.aggregate.ReseveStatus;

public interface IMettingCommandService {

    ReseveStatus reserve(MettingReserveCommand reserve);

    CancelReservationStatusEnum cancel(MettingCancelCommand cancel);

    MettingDTO get(MettingGetCommand get);

}
