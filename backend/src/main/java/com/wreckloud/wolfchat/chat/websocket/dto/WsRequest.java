package com.wreckloud.wolfchat.chat.websocket.dto;

import lombok.Data;

/**
 * @Description WebSocket 请求消息
 * @Author Wreckloud
 * @Date 2026-02-05
 */
@Data
public class WsRequest {
    /**
     * 消息类型：AUTH / SEND
     */
    private String type;

    /**
     * 授权 token（AUTH 消息使用）
     */
    private String token;

    /**
     * 客户端消息ID（用于 ACK 对应）
     */
    private String clientMsgId;

    /**
     * 会话ID（SEND 消息使用）
     */
    private Long conversationId;

    /**
     * 消息内容
     */
    private String content;
}
