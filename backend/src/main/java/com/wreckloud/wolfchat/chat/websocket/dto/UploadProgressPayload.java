package com.wreckloud.wolfchat.chat.websocket.dto;

import com.wreckloud.wolfchat.chat.message.domain.enums.MessageType;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.time.LocalDateTime;

/**
 * @Description 媒体上传进度推送载荷
 * @Author Wreckloud
 * @Date 2026-03-20
 */
@Data
@Schema(description = "媒体上传进度")
public class UploadProgressPayload {
    @Schema(description = "发送场景：SEND / LOBBY_SEND")
    private String sendType;

    @Schema(description = "会话ID（仅私聊场景）")
    private Long conversationId;

    @Schema(description = "客户端消息ID")
    private String clientMsgId;

    @Schema(description = "发送者ID")
    private Long senderId;

    @Schema(description = "发送者狼藉号")
    private String senderWolfNo;

    @Schema(description = "发送者行者名")
    private String senderNickname;

    @Schema(description = "发送者头衔名称")
    private String senderEquippedTitleName;

    @Schema(description = "发送者头衔颜色")
    private String senderEquippedTitleColor;

    @Schema(description = "发送者头像")
    private String senderAvatar;

    @Schema(description = "消息类型")
    private MessageType msgType;

    @Schema(description = "上传进度（0-100）")
    private Integer uploadProgress;

    @Schema(description = "上传状态：UPLOADING / SENDING / FAILED")
    private String uploadStatus;

    @Schema(description = "时间")
    private LocalDateTime createTime;
}

