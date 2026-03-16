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
    REPLY_LIKED("REPLY_LIKED", "点赞", "THREAD");

    @EnumValue
    @JsonValue
    private final String value;

    private final String label;

    private final String bizType;
}
