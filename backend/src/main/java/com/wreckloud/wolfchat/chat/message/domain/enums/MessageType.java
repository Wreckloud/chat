package com.wreckloud.wolfchat.chat.message.domain.enums;

import com.baomidou.mybatisplus.annotation.EnumValue;
import com.fasterxml.jackson.annotation.JsonValue;
import lombok.AllArgsConstructor;
import lombok.Getter;

/**
 * @Description 消息类型枚举
 * @Author Wreckloud
 * @Date 2026-01-29
 */
@Getter
@AllArgsConstructor
public enum MessageType {
    /**
     * 文本消息
     */
    TEXT("TEXT");

    @EnumValue
    @JsonValue
    private final String value;
}

