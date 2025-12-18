package com.wreckloud.wolfchat.group.domain.enums;

import lombok.AllArgsConstructor;
import lombok.Getter;

/**
 * @Description 群成员角色枚举
 *
 * @author Wreckloud
 * @date 2024-12-18
 */
@Getter
@AllArgsConstructor
public enum GroupMemberRole {

    /**
     * 群主
     */
    OWNER(1, "群主"),

    /**
     * 管理员
     */
    ADMIN(2, "管理员"),

    /**
     * 普通成员
     */
    MEMBER(3, "普通成员");

    /**
     * 角色代码
     */
    private final Integer code;

    /**
     * 角色描述
     */
    private final String desc;

    /**
     * 根据角色代码获取枚举
     *
     * @param code 角色代码
     * @return 枚举值，未找到返回null
     */
    public static GroupMemberRole getByCode(Integer code) {
        if (code == null) {
            return null;
        }
        for (GroupMemberRole role : values()) {
            if (role.getCode().equals(code)) {
                return role;
            }
        }
        return null;
    }

    /**
     * 判断角色代码是否有效
     *
     * @param code 角色代码
     * @return 是否有效
     */
    public static boolean isValid(Integer code) {
        return getByCode(code) != null;
    }

    /**
     * 判断是否是群主
     *
     * @param code 角色代码
     * @return 是否是群主
     */
    public static boolean isOwner(Integer code) {
        return OWNER.getCode().equals(code);
    }

    /**
     * 判断是否是管理员（包含群主）
     *
     * @param code 角色代码
     * @return 是否是管理员
     */
    public static boolean isAdminOrOwner(Integer code) {
        return OWNER.getCode().equals(code) || ADMIN.getCode().equals(code);
    }
}

