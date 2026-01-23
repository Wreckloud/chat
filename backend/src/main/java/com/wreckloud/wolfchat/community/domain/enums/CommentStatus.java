package com.wreckloud.wolfchat.community.domain.enums;

import com.baomidou.mybatisplus.annotation.EnumValue;
import com.fasterxml.jackson.annotation.JsonValue;
import lombok.Getter;

/**
 * @Description 评论状态枚举
 * @Author Wreckloud
 * @Date 2026-01-23
 */
@Getter
public enum CommentStatus {
    /**
     * 正常
     */
    NORMAL("NORMAL"),

    /**
     * 已删除
     */
    DELETED("DELETED");

    /**
     * 存储到数据库的值，同时用于 JSON 序列化
     */
    @EnumValue
    @JsonValue
    private final String value;

    CommentStatus(String value) {
        this.value = value;
    }
}

