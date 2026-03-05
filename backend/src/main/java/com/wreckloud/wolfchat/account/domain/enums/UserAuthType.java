package com.wreckloud.wolfchat.account.domain.enums;

import com.baomidou.mybatisplus.annotation.EnumValue;
import com.fasterxml.jackson.annotation.JsonValue;
import lombok.Getter;

/**
 * @Description 用户认证类型
 * @Author Wreckloud
 * @Date 2026-03-05
 */
@Getter
public enum UserAuthType {
    /**
     * 狼藉号 + 密码
     */
    WOLF_NO_PASSWORD("WOLF_NO_PASSWORD"),

    /**
     * 邮箱 + 密码
     */
    EMAIL_PASSWORD("EMAIL_PASSWORD");

    @EnumValue
    @JsonValue
    private final String value;

    UserAuthType(String value) {
        this.value = value;
    }
}
