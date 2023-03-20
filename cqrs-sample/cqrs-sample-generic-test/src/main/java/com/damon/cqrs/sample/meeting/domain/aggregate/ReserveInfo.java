package com.damon.cqrs.sample.meeting.domain.aggregate;

import com.damon.cqrs.domain.ValueObject;
import lombok.Getter;

@Getter
public class ReserveInfo extends ValueObject {

    private Long userId;

    private int start;

    private int end;

    private String mettingTopic;

    private String mettingContent;

    private String attachmentUrl;

    public ReserveInfo() {

    }

    public ReserveInfo(Long userId,
                       int start,
                       int end,
                       String mettingTopic,
                       String mettingContent,
                       String attachmentUrl
    ) {
        this.userId = userId;
        this.start = start;
        this.end = end;
        this.mettingTopic = mettingTopic;
        this.mettingContent = mettingContent;
        this.attachmentUrl = attachmentUrl;
    }

}
