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
     * 消息类型：AUTH / SEND / RECALL / LOBBY_SEND / LOBBY_RECALL / UPLOAD_PROGRESS / PING
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
     * 视频封面对象 Key
     */
    private String mediaPosterKey;

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

    /**
     * 发送场景：SEND / LOBBY_SEND（上传进度同步使用）
     */
    private String sendType;

    /**
     * 上传进度（0-100）
     */
    private Integer uploadProgress;

    /**
     * 上传状态：UPLOADING / SENDING / FAILED
     */
    private String uploadStatus;

    /**
     * 操作目标消息ID（RECALL 消息使用）
     */
    private Long messageId;
}
