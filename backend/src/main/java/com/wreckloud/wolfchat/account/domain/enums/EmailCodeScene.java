package com.wreckloud.wolfchat.account.domain.enums;

import lombok.AllArgsConstructor;
import lombok.Getter;

import java.util.Locale;

/**
 * @Description 邮箱验证码场景
 * @Author Wreckloud
 * @Date 2026-03-04
 */
@Getter
@AllArgsConstructor
public enum EmailCodeScene {
    BIND_EMAIL("BIND_EMAIL", "邮箱认证"),
    RESET_PASSWORD("RESET_PASSWORD", "重置密码");

    private final String code;
    private final String title;

    public static EmailCodeScene fromCode(String code) {
        if (code == null) {
            return null;
        }
        String normalizedCode = code.trim().toUpperCase(Locale.ROOT);
        for (EmailCodeScene scene : values()) {
            if (scene.code.equals(normalizedCode)) {
                return scene;
            }
        }
        return null;
    }
}
