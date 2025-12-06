package com.wreckloud.wolfchat.account.domain.enums;

import lombok.AllArgsConstructor;
import lombok.Getter;

/**
 * @Description 用户状态枚举
 *
 * @author Wreckloud
 * @date 2025-12-06
 */
@Getter
@AllArgsConstructor
public enum UserStatus {

    /**
     * 正常
     */
    NORMAL(1, "正常"),

    /**
     * 禁用
     */
    DISABLED(2, "禁用"),

    /**
     * 注销
     */
    CANCELLED(3, "注销");

    /**
     * 状态码
     */
    private final Integer code;

    /**
     * 状态描述
     */
    private final String desc;

    /**
     * 根据状态码获取枚举
     *
     * @param code 状态码
     * @return 枚举值，未找到返回null
     */
    public static UserStatus getByCode(Integer code) {
        if (code == null) {
            return null;
        }
        for (UserStatus status : values()) {
            if (status.getCode().equals(code)) {
                return status;
            }
        }
        return null;
    }

    /**
     * 判断状态码是否有效
     *
     * @param code 状态码
     * @return 是否有效
     */
    public static boolean isValid(Integer code) {
        return getByCode(code) != null;
    }
}

