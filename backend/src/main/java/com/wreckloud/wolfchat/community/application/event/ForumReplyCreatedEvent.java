package com.wreckloud.wolfchat.community.application.event;

import lombok.Getter;

/**
 * 回帖发布事件。
 */
@Getter
public class ForumReplyCreatedEvent {
    private final Long replyId;
    private final Long threadId;
    private final Long authorId;
    private final Long quoteReplyId;
    private final String content;

    public ForumReplyCreatedEvent(Long replyId, Long threadId, Long authorId, Long quoteReplyId, String content) {
        this.replyId = replyId;
        this.threadId = threadId;
        this.authorId = authorId;
        this.quoteReplyId = quoteReplyId;
        this.content = content;
    }
}

