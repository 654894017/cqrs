package com.damon.cqrs.sample.metting;

import cn.hutool.core.util.IdUtil;
import com.damon.cqrs.CqrsConfig;
import com.damon.cqrs.sample.TestConfig;
import com.damon.cqrs.sample.metting.api.command.MeetingId;
import com.damon.cqrs.sample.metting.api.command.MettingGetCommand;
import com.damon.cqrs.sample.metting.api.command.MettingReserveCommand;
import com.damon.cqrs.sample.metting.domain.MettingCommandService;
import com.damon.cqrs.sample.metting.domain.aggregate.MettingTime;
import com.damon.cqrs.sample.metting.domain.aggregate.ReseveStatus;

import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class MettingTest5 {

    public static void main(String[] args) throws NoSuchMethodException, ExecutionException, InterruptedException {
        CqrsConfig cqrsConfig = TestConfig.init();
        MettingCommandService mettingCommandService = new MettingCommandService(cqrsConfig);
        Long userId = 181987L;
        String meetingDate = "20230320";
        String mettingNumber = "1103";
        MeetingId meetingId = new MeetingId(meetingDate, mettingNumber);
        ExecutorService service = Executors.newFixedThreadPool(100);
        for (int i = 0; i < 100; i++) {
            int finalI = i;
            int finalI1 = i;
            service.submit(() -> {
                MettingReserveCommand reserveCommand = new MettingReserveCommand(
                        //预定 0点到 10点会议(大等于0小于9:59)
                        IdUtil.getSnowflakeNextId(), meetingId.getId(), userId,
                        new MettingTime(finalI, finalI1 + 1), "UC权限接入议题",
                        meetingDate,
                        mettingNumber,
                        "UC权限接入议题",
                        "http://xxxxxxxx.xlsx"
                );
                //预定1103会议室
                ReseveStatus reseveStatus = mettingCommandService.reserve(reserveCommand);
                System.out.println(reseveStatus.getReserveStatusEnum());
                System.out.println(mettingCommandService.get(new MettingGetCommand(IdUtil.getSnowflakeNextId(), meetingId.getId())));
            }).get();
        }

    }

}
