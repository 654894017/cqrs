package com.damon.cqrs.sample.meeting.api.command;

import com.damon.cqrs.domain.Command;
import com.damon.cqrs.sample.meeting.domain.aggregate.MettingTime;
import lombok.NonNull;

public class MettingReserveCommand extends Command {

    private Long userId;
    private MettingTime mettingTime;
    private String mettingTopic;
    private String mettingContent;
    private String attachmentUrl;

    public MettingReserveCommand(@NonNull Long commandId,
                                 @NonNull Long aggregateId,
                                 @NonNull Long userId,
                                 @NonNull MettingTime mettingTime,
                                 @NonNull String mettingTopic,
                                 String mettingContent,
                                 String attachmentUrl
    ) {
        super(commandId, aggregateId);
        this.mettingTime = mettingTime;
        this.userId = userId;
        this.mettingTopic = mettingTopic;
        this.mettingContent = mettingContent;
        this.attachmentUrl = attachmentUrl;
    }

    public MettingTime getMettingTime() {
        return mettingTime;
    }

    public String getMettingTopic() {
        return mettingTopic;
    }

    public String getMettingContent() {
        return mettingContent;
    }

    public String getAttachmentUrl() {
        return attachmentUrl;
    }

    public Long getUserId() {
        return userId;
    }

}
