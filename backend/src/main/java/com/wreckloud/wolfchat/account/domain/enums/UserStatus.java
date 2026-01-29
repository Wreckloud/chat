package com.wreckloud.wolfchat.account.domain.enums;

import com.baomidou.mybatisplus.annotation.EnumValue;
import com.fasterxml.jackson.annotation.JsonValue;
import lombok.Getter;

/**
 * @Description 行者状态枚举
 * @Author Wreckloud
 * @Date 2026-01-23
 */
@Getter
public enum UserStatus {
    /**
     * 正常
     */
    NORMAL("NORMAL"),

    /**
     * 禁用
     */
    DISABLED("DISABLED");

    /**
     * 存储到数据库的值，同时用于 JSON 序列化
     */
    @EnumValue
    @JsonValue
    private final String value;

    UserStatus(String value) {
        this.value = value;
    }
}

