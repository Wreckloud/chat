package com.wreckloud.wolfchat.chat.message.domain.enums;

import com.baomidou.mybatisplus.annotation.EnumValue;
import com.fasterxml.jackson.annotation.JsonValue;
import lombok.Getter;

/**
 * @Description 消息送达状态
 * @Author Wreckloud
 * @Date 2026-03-02
 */
@Getter
public enum MessageDeliveryStatus {
    /**
     * 未送达
     */
    UNDELIVERED(0),
    /**
     * 已送达
     */
    DELIVERED(1);

    /**
     * 存储到数据库的值，同时用于 JSON 序列化
     */
    @EnumValue
    @JsonValue
    private final Integer value;

    MessageDeliveryStatus(Integer value) {
        this.value = value;
    }
}
