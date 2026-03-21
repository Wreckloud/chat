package com.wreckloud.wolfchat.follow.application.event;

import lombok.Getter;

/**
 * 用户关注事件。
 */
@Getter
public class UserFollowedEvent {
    private final Long followerId;
    private final Long followeeId;

    public UserFollowedEvent(Long followerId, Long followeeId) {
        this.followerId = followerId;
        this.followeeId = followeeId;
    }
}

