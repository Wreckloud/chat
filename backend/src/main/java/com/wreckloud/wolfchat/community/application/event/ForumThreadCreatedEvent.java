package com.wreckloud.wolfchat.community.application.event;

import lombok.Getter;

/**
 * 主题发布事件（仅已发布主题）。
 */
@Getter
public class ForumThreadCreatedEvent {
    private final Long threadId;
    private final Long authorId;
    private final String title;
    private final String content;

    public ForumThreadCreatedEvent(Long threadId, Long authorId, String title, String content) {
        this.threadId = threadId;
        this.authorId = authorId;
        this.title = title;
        this.content = content;
    }
}

