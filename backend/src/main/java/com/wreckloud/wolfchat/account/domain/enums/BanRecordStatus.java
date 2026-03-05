package com.wreckloud.wolfchat.account.domain.enums;

import com.baomidou.mybatisplus.annotation.EnumValue;
import com.fasterxml.jackson.annotation.JsonValue;
import lombok.Getter;

/**
 * @Description 封禁记录状态
 * @Author Wreckloud
 * @Date 2026-03-05
 */
@Getter
public enum BanRecordStatus {
    /**
     * 封禁生效中
     */
    ACTIVE("ACTIVE"),

    /**
     * 已解除
     */
    LIFTED("LIFTED"),

    /**
     * 已到期
     */
    EXPIRED("EXPIRED");

    @EnumValue
    @JsonValue
    private final String value;

    BanRecordStatus(String value) {
        this.value = value;
    }
}
