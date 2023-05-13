package com.damon.cqrs.sample.metting.api.command;

import com.damon.cqrs.domain.Command;

public class MettingCreateCommand extends Command {

    private final String meetingDate;

    public MettingCreateCommand(Long commandId, Long meetingId, String meetingDate) {
        super(commandId, meetingId);
        this.meetingDate = meetingDate;
    }

    public String getMeetingDate() {
        return meetingDate;
    }


}
