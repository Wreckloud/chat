package com.wreckloud.wolfchat.account.domain.enums;

import com.baomidou.mybatisplus.annotation.EnumValue;
import com.fasterxml.jackson.annotation.JsonValue;
import lombok.Getter;

/**
 * @Description 登录结果
 * @Author Wreckloud
 * @Date 2026-03-05
 */
@Getter
public enum LoginResult {
    /**
     * 登录成功
     */
    SUCCESS("SUCCESS"),

    /**
     * 登录失败
     */
    FAIL("FAIL");

    @EnumValue
    @JsonValue
    private final String value;

    LoginResult(String value) {
        this.value = value;
    }
}
