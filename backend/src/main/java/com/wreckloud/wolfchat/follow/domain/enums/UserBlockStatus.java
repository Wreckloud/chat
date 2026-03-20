package com.wreckloud.wolfchat.follow.domain.enums;

import com.baomidou.mybatisplus.annotation.EnumValue;
import lombok.Getter;

/**
 * 用户拉黑关系状态
 */
@Getter
public enum UserBlockStatus {
    /**
     * 已拉黑
     */
    BLOCKED("BLOCKED"),
    /**
     * 已解除
     */
    UNBLOCKED("UNBLOCKED");

    @EnumValue
    private final String value;

    UserBlockStatus(String value) {
        this.value = value;
    }
}

