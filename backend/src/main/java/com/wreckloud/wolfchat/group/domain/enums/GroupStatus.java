package com.wreckloud.wolfchat.group.domain.enums;

import lombok.AllArgsConstructor;
import lombok.Getter;

/**
 * @Description 群组状态枚举
 *
 * @author Wreckloud
 * @date 2024-12-18
 */
@Getter
@AllArgsConstructor
public enum GroupStatus {

    /**
     * 正常
     */
    NORMAL(1, "正常"),

    /**
     * 已解散
     */
    DISBANDED(2, "已解散");

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
    public static GroupStatus getByCode(Integer code) {
        if (code == null) {
            return null;
        }
        for (GroupStatus status : values()) {
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

    /**
     * 判断是否是正常状态
     *
     * @param code 状态码
     * @return 是否正常
     */
    public static boolean isNormal(Integer code) {
        return NORMAL.getCode().equals(code);
    }
}

