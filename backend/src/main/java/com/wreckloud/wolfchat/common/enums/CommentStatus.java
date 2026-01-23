package com.wreckloud.wolfchat.common.enums;

import com.baomidou.mybatisplus.annotation.EnumValue;
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
     * 存储到数据库的值
     */
    @EnumValue
    private final String value;

    CommentStatus(String value) {
        this.value = value;
    }
}

