package com.wreckloud.wolfchat.chat.message.domain.enums;

import com.baomidou.mybatisplus.annotation.EnumValue;
import com.fasterxml.jackson.annotation.JsonValue;
import lombok.Getter;

/**
 * @Description 消息类型枚举
 * @Author Wreckloud
 * @Date 2026-01-29
 */
@Getter
public enum MessageType {
    /**
     * 文本消息
     */
    TEXT("TEXT"),

    /**
     * 图片消息
     */
    IMAGE("IMAGE"),

    /**
     * 视频消息
     */
    VIDEO("VIDEO"),

    /**
     * 文件消息
     */
    FILE("FILE"),

    /**
     * 撤回消息（服务端更新原消息类型）
     */
    RECALL("RECALL");

    /**
     * 存储到数据库的值，同时用于 JSON 序列化
     */
    @EnumValue
    @JsonValue
    private final String value;

    MessageType(String value) {
        this.value = value;
    }
}

