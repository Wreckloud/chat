package com.wreckloud.wolfchat.community.domain.enums;

import com.baomidou.mybatisplus.annotation.EnumValue;
import com.fasterxml.jackson.annotation.JsonValue;
import lombok.Getter;

/**
 * @Description 主题状态枚举
 * @Author Wreckloud
 * @Date 2026-03-10
 */
@Getter
public enum ForumThreadStatus {
    /**
     * 草稿
     */
    DRAFT("DRAFT"),

    /**
     * 正常
     */
    NORMAL("NORMAL"),

    /**
     * 已锁定
     */
    LOCKED("LOCKED"),

    /**
     * 已删除（垃圾站）
     */
    DELETED("DELETED"),

    /**
     * 已彻底删除（逻辑删除，用户不可见）
     */
    PURGED("PURGED");

    @EnumValue
    @JsonValue
    private final String value;

    ForumThreadStatus(String value) {
        this.value = value;
    }
}
