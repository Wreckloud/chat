package com.wreckloud.wolfchat.notice.domain.enums;

import com.baomidou.mybatisplus.annotation.EnumValue;
import com.fasterxml.jackson.annotation.JsonValue;
import lombok.AllArgsConstructor;
import lombok.Getter;

/**
 * @Description 系统通知类型
 * @Author Wreckloud
 * @Date 2026-03-16
 */
@Getter
@AllArgsConstructor
public enum NoticeType {
    /**
     * 成就解锁
     */
    ACHIEVEMENT_UNLOCK("ACHIEVEMENT_UNLOCK", "成就", "ACHIEVEMENT"),

    /**
     * 收到关注
     */
    FOLLOW_RECEIVED("FOLLOW_RECEIVED", "关注", "FOLLOW"),

    /**
     * 主题被点赞
     */
    THREAD_LIKED("THREAD_LIKED", "点赞", "THREAD"),

    /**
     * 主题收到回复
     */
    THREAD_REPLIED("THREAD_REPLIED", "回复", "THREAD"),

    /**
     * 回复被点赞
     */
    REPLY_LIKED("REPLY_LIKED", "点赞", "THREAD"),

    /**
     * 回复收到新的回复（楼中楼）
     */
    REPLY_REPLIED("REPLY_REPLIED", "回复", "THREAD"),

    /**
     * 私聊消息被回复
     */
    CHAT_MESSAGE_REPLIED("CHAT_MESSAGE_REPLIED", "回复", "CHAT"),

    /**
     * 大厅消息被回复
     */
    LOBBY_MESSAGE_REPLIED("LOBBY_MESSAGE_REPLIED", "回复", "LOBBY");

    @EnumValue
    @JsonValue
    private final String value;

    private final String label;

    private final String bizType;
}
