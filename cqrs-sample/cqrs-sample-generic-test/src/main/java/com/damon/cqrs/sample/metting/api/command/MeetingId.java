package com.damon.cqrs.sample.metting.api.command;

import lombok.NonNull;

public class MeetingId {
    private final String meetingDate;
    private final String meettingNumber;

    public MeetingId(@NonNull String meetingDate, @NonNull String meettingNumber) {
        this.meetingDate = meetingDate;
        this.meettingNumber = meettingNumber;
    }

    public Long getId() {
        return Long.parseLong(meetingDate + meettingNumber);
    }
}
