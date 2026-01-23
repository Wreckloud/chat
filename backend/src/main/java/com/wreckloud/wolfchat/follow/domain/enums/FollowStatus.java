package com.wreckloud.wolfchat.follow.domain.enums;

import com.baomidou.mybatisplus.annotation.EnumValue;
import com.fasterxml.jackson.annotation.JsonValue;
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
     * 存储到数据库的值，同时用于 JSON 序列化
     */
    @EnumValue
    @JsonValue
    private final String value;

    FollowStatus(String value) {
        this.value = value;
    }
}

