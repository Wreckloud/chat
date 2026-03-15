package com.wreckloud.wolfchat.chat.presence.application.event;

import lombok.Getter;

import java.time.LocalDateTime;

/**
 * @Description 用户在线状态变更事件
 * @Author Wreckloud
 * @Date 2026-03-07
 */
@Getter
public class UserPresenceChangedEvent {
    private final Long userId;
    private final boolean online;
    private final LocalDateTime lastSeenAt;

    public UserPresenceChangedEvent(Long userId, boolean online, LocalDateTime lastSeenAt) {
        this.userId = userId;
        this.online = online;
        this.lastSeenAt = lastSeenAt;
    }
}
