package com.wreckloud.wolfchat.chat.websocket.dto;

import com.wreckloud.wolfchat.chat.websocket.enums.WsType;
import lombok.Data;

/**
 * @Description WebSocket 响应消息
 * @Author Wreckloud
 * @Date 2026-02-05
 */
@Data
public class WsResponse {
    /**
     * 消息类型：AUTH_OK / ACK / MESSAGE / LOBBY_MESSAGE / UPLOAD_PROGRESS / PRESENCE / ERROR
     */
    private WsType type;

    /**
     * 业务错误码（ERROR 时使用）
     */
    private Integer code;

    /**
     * 业务错误消息（ERROR 时使用）
     */
    private String message;

    /**
     * 客户端消息ID（用于 ACK 对应）
     */
    private String clientMsgId;

    /**
     * 业务数据（MessageVO 等）
     */
    private Object data;
}
