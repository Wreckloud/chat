package com.wreckloud.wolfchat.common.enums;

import com.baomidou.mybatisplus.annotation.EnumValue;
import lombok.Getter;

/**
 * @Description 关注状态枚举
 * @Author Wreckloud
 * @Date 2026-01-23
 */
@Getter
public enum FollowStatus {
    /**
     * 关注中
     */
    FOLLOWING("FOLLOWING"),

    /**
     * 已取消关注
     */
    UNFOLLOWED("UNFOLLOWED");

    /**
     * 存储到数据库的值
     */
    @EnumValue
    private final String value;

    FollowStatus(String value) {
        this.value = value;
    }
}

