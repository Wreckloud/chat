package com.wreckloud.wolfchat.account.domain.enums;

import com.baomidou.mybatisplus.annotation.EnumValue;
import com.fasterxml.jackson.annotation.JsonValue;
import lombok.Getter;

/**
 * @Description 登录方式
 * @Author Wreckloud
 * @Date 2026-03-05
 */
@Getter
public enum LoginMethod {
    /**
     * 狼藉号登录
     */
    WOLF_NO("WOLF_NO"),

    /**
     * 邮箱登录
     */
    EMAIL("EMAIL"),

    /**
     * 未识别登录方式（例如空账号）
     */
    UNKNOWN("UNKNOWN");

    @EnumValue
    @JsonValue
    private final String value;

    LoginMethod(String value) {
        this.value = value;
    }
}
