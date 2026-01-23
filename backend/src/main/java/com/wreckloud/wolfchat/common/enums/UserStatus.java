package com.wreckloud.wolfchat.common.enums;

import com.baomidou.mybatisplus.annotation.EnumValue;
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
     * 存储到数据库的值
     */
    @EnumValue
    private final String value;

    UserStatus(String value) {
        this.value = value;
    }
}

