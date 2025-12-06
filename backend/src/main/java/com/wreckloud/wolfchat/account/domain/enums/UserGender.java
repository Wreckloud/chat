package com.wreckloud.wolfchat.account.domain.enums;

import lombok.AllArgsConstructor;
import lombok.Getter;

/**
 * @Description 用户性别枚举
 *
 * @author Wreckloud
 * @date 2025-12-06
 */
@Getter
@AllArgsConstructor
public enum UserGender {

    /**
     * 未知
     */
    UNKNOWN(0, "未知"),

    /**
     * 男
     */
    MALE(1, "男"),

    /**
     * 女
     */
    FEMALE(2, "女");

    /**
     * 性别码
     */
    private final Integer code;

    /**
     * 性别描述
     */
    private final String desc;

    /**
     * 根据性别码获取枚举
     *
     * @param code 性别码
     * @return 枚举值，未找到返回null
     */
    public static UserGender getByCode(Integer code) {
        if (code == null) {
            return null;
        }
        for (UserGender gender : values()) {
            if (gender.getCode().equals(code)) {
                return gender;
            }
        }
        return null;
    }

    /**
     * 判断性别码是否有效
     *
     * @param code 性别码
     * @return 是否有效
     */
    public static boolean isValid(Integer code) {
        return getByCode(code) != null;
    }
}

