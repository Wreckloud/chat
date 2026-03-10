package com.wreckloud.wolfchat.chat.message.api.vo;

import com.wreckloud.wolfchat.chat.message.domain.enums.MessageType;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.time.LocalDateTime;

/**
 * @Description 消息 VO
 * @Author Wreckloud
 * @Date 2026-01-29
 */
@Data
@Schema(description = "消息信息")
public class MessageVO {
    @Schema(description = "消息ID")
    private Long messageId;

    @Schema(description = "会话ID")
    private Long conversationId;

    @Schema(description = "发送者ID")
    private Long senderId;

    @Schema(description = "发送者狼藉号")
    private String senderWolfNo;

    @Schema(description = "发送者行者名")
    private String senderNickname;

    @Schema(description = "发送者头像")
    private String senderAvatar;

    @Schema(description = "接收者ID")
    private Long receiverId;

    @Schema(description = "消息内容")
    private String content;

    @Schema(description = "消息类型")
    private MessageType msgType;

    @Schema(description = "媒体对象Key")
    private String mediaKey;

    @Schema(description = "媒体访问地址")
    private String mediaUrl;

    @Schema(description = "媒体宽度")
    private Integer mediaWidth;

    @Schema(description = "媒体高度")
    private Integer mediaHeight;

    @Schema(description = "媒体大小")
    private Long mediaSize;

    @Schema(description = "媒体 MIME 类型")
    private String mediaMimeType;

    @Schema(description = "发送时间")
    private LocalDateTime createTime;
}

