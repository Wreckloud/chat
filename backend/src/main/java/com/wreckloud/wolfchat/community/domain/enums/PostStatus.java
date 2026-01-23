package com.wreckloud.wolfchat.community.domain.enums;

import com.baomidou.mybatisplus.annotation.EnumValue;
import lombok.Getter;

/**
 * @Description 帖子状态枚举
 * @Author Wreckloud
 * @Date 2026-01-23
 */
@Getter
public enum PostStatus {
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

    PostStatus(String value) {
        this.value = value;
    }
}

