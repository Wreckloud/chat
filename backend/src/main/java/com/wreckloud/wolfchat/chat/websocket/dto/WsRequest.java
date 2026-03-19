package com.wreckloud.wolfchat.chat.websocket.dto;

import com.wreckloud.wolfchat.chat.message.domain.enums.MessageType;
import com.wreckloud.wolfchat.chat.websocket.enums.WsType;
import lombok.Data;

/**
 * @Description WebSocket 请求消息
 * @Author Wreckloud
 * @Date 2026-02-05
 */
@Data
public class WsRequest {
    /**
     * 消息类型：AUTH / SEND / LOBBY_SEND / PING
     */
    private WsType type;

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

    /**
     * 消息类型：TEXT / IMAGE / VIDEO / FILE
     */
    private MessageType msgType;

    /**
     * 媒体对象 Key
     */
    private String mediaKey;

    /**
     * 媒体宽度
     */
    private Integer mediaWidth;

    /**
     * 媒体高度
     */
    private Integer mediaHeight;

    /**
     * 媒体大小
     */
    private Long mediaSize;

    /**
     * 媒体 MIME 类型
     */
    private String mediaMimeType;

    /**
     * 回复目标消息ID
     */
    private Long replyToMessageId;
}
