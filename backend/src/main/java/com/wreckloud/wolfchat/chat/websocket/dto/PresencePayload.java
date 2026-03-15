package com.wreckloud.wolfchat.chat.websocket.dto;

import lombok.Data;

import java.time.LocalDateTime;

/**
 * @Description 在线状态推送载荷
 * @Author Wreckloud
 * @Date 2026-03-07
 */
@Data
public class PresencePayload {
    private Long userId;
    private Boolean online;
    private LocalDateTime lastSeenAt;
}
