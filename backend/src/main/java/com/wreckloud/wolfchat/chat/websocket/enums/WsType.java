package com.wreckloud.wolfchat.chat.websocket.enums;

/**
 * @Description WebSocket 消息类型
 * @Author Wreckloud
 * @Date 2026-02-15
 */
public enum WsType {
    /**
     * 客户端认证
     */
    AUTH,
    /**
     * 发送消息
     */
    SEND,
    /**
     * 心跳
     */
    PING,
    /**
     * 认证成功
     */
    AUTH_OK,
    /**
     * 发送回执
     */
    ACK,
    /**
     * 推送消息
     */
    MESSAGE,
    /**
     * 在线状态推送
     */
    PRESENCE,
    /**
     * 错误消息
     */
    ERROR
}
