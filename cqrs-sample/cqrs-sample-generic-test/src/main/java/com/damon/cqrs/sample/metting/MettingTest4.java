package com.damon.cqrs.sample.metting;

import cn.hutool.core.util.IdUtil;
import com.damon.cqrs.config.CqrsConfig;
import com.damon.cqrs.sample.TestConfig;
import com.damon.cqrs.sample.metting.api.command.MeetingId;
import com.damon.cqrs.sample.metting.api.command.MettingGetCommand;
import com.damon.cqrs.sample.metting.api.command.MettingReserveCommand;
import com.damon.cqrs.sample.metting.domain.MettingCommandService;
import com.damon.cqrs.sample.metting.domain.aggregate.MettingTime;
import com.damon.cqrs.sample.metting.domain.aggregate.ReseveStatus;

public class MettingTest4 {

    public static void main(String[] args) throws NoSuchMethodException {
        CqrsConfig cqrsConfig = TestConfig.init();
        MettingCommandService mettingCommandHandler = new MettingCommandService(cqrsConfig);
        Long userId = 181987L;
        String meetingDate = "20230320";
        String mettingNumber = "1103";
        MeetingId meetingId = new MeetingId(meetingDate, mettingNumber);
        MettingReserveCommand reserveCommand = new MettingReserveCommand(
                //预定 0点到 10点会议(大等于0小于9:59)
                IdUtil.getSnowflakeNextId(), meetingId.getId(), userId,
                new MettingTime(0 * 60, 10 * 60), "UC权限接入议题",
                meetingDate,
                mettingNumber,
                "UC权限接入议题",
                "http://xxxxxxxx.xlsx"
        );
        //预定1103会议室
        ReseveStatus reseveStatus = mettingCommandHandler.reserve(reserveCommand);
        System.out.println(mettingCommandHandler.get(new MettingGetCommand(IdUtil.getSnowflakeNextId(), meetingId.getId())));
    }

}
