package com.wreckloud.wolfchat.community.domain.enums;

import com.baomidou.mybatisplus.annotation.EnumValue;
import com.fasterxml.jackson.annotation.JsonValue;
import lombok.Getter;

/**
 * @Description 主题类型枚举
 * @Author Wreckloud
 * @Date 2026-03-10
 */
@Getter
public enum ForumThreadType {
    /**
     * 普通主题
     */
    NORMAL("NORMAL"),

    /**
     * 置顶主题
     */
    STICKY("STICKY"),

    /**
     * 公告主题
     */
    ANNOUNCEMENT("ANNOUNCEMENT");

    @EnumValue
    @JsonValue
    private final String value;

    ForumThreadType(String value) {
        this.value = value;
    }
}
