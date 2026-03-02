package com.wreckloud.wolfchat.chat.websocket.enums;

import lombok.Getter;

/**
 * @Description WebSocket 消息类型
 * @Author Wreckloud
 * @Date 2026-02-15
 */
@Getter
public enum WsType {
    /**
     * 客户端认证
     */
    AUTH("AUTH"),
    /**
     * 发送消息
     */
    SEND("SEND"),
    /**
     * 认证成功
     */
    AUTH_OK("AUTH_OK"),
    /**
     * 发送回执
     */
    ACK("ACK"),
    /**
     * 推送消息
     */
    MESSAGE("MESSAGE"),
    /**
     * 错误消息
     */
    ERROR("ERROR");

    private final String value;

    WsType(String value) {
        this.value = value;
    }

    /**
     * 从字符串解析消息类型
     */
    public static WsType fromValue(String value) {
        if (value == null) {
            return null;
        }
        for (WsType type : values()) {
            if (type.value.equals(value)) {
                return type;
            }
        }
        return null;
    }
}
