package com.wreckloud.wolfchat.community.domain.enums;

import com.baomidou.mybatisplus.annotation.EnumValue;
import com.fasterxml.jackson.annotation.JsonValue;
import lombok.Getter;

/**
 * @Description 回复状态枚举
 * @Author Wreckloud
 * @Date 2026-03-10
 */
@Getter
public enum ForumReplyStatus {
    /**
     * 正常
     */
    NORMAL("NORMAL"),

    /**
     * 已删除
     */
    DELETED("DELETED");

    @EnumValue
    @JsonValue
    private final String value;

    ForumReplyStatus(String value) {
        this.value = value;
    }
}
