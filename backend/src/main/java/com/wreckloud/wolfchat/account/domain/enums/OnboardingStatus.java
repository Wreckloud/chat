package com.wreckloud.wolfchat.account.domain.enums;

import com.baomidou.mybatisplus.annotation.EnumValue;
import com.fasterxml.jackson.annotation.JsonValue;
import lombok.Getter;

/**
 * @Description 新用户引导状态
 * @Author Wreckloud
 * @Date 2026-03-05
 */
@Getter
public enum OnboardingStatus {
    /**
     * 待引导
     */
    PENDING("PENDING"),

    /**
     * 已完成引导
     */
    COMPLETED("COMPLETED"),

    /**
     * 已跳过引导
     */
    SKIPPED("SKIPPED");

    @EnumValue
    @JsonValue
    private final String value;

    OnboardingStatus(String value) {
        this.value = value;
    }
}
