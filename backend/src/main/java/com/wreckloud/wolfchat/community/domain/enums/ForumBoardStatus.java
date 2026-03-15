package com.wreckloud.wolfchat.community.domain.enums;

import com.baomidou.mybatisplus.annotation.EnumValue;
import com.fasterxml.jackson.annotation.JsonValue;
import lombok.Getter;

/**
 * @Description 版块状态枚举
 * @Author Wreckloud
 * @Date 2026-03-10
 */
@Getter
public enum ForumBoardStatus {
    /**
     * 正常开放
     */
    NORMAL("NORMAL"),

    /**
     * 已关闭发帖
     */
    CLOSED("CLOSED");

    @EnumValue
    @JsonValue
    private final String value;

    ForumBoardStatus(String value) {
        this.value = value;
    }
}
