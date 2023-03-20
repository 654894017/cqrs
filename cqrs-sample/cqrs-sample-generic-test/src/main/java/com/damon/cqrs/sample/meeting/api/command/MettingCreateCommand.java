package com.damon.cqrs.sample.meeting.api.command;

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
