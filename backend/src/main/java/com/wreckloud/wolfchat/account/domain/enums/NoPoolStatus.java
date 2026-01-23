package com.wreckloud.wolfchat.account.domain.enums;

import com.baomidou.mybatisplus.annotation.EnumValue;
import lombok.Getter;

/**
 * @Description 狼藉号池状态枚举
 * @Author Wreckloud
 * @Date 2026-01-23
 */
@Getter
public enum NoPoolStatus {
    /**
     * 未使用
     */
    UNUSED("UNUSED"),

    /**
     * 已使用
     */
    USED("USED"),

    /**
     * 预留
     */
    RESERVED("RESERVED");

    /**
     * 存储到数据库的值
     */
    @EnumValue
    private final String value;

    NoPoolStatus(String value) {
        this.value = value;
    }
}

