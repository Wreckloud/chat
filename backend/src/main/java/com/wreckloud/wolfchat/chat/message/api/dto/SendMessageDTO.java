package com.wreckloud.wolfchat.chat.message.api.dto;

import com.wreckloud.wolfchat.chat.message.domain.enums.MessageType;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

/**
 * @Description 发送消息 DTO
 * @Author Wreckloud
 * @Date 2026-01-29
 */
@Data
@Schema(description = "发送消息请求")
public class SendMessageDTO {
    @Schema(description = "消息内容")
    private String content;

    @Schema(description = "消息类型：TEXT/IMAGE/VIDEO/FILE")
    private MessageType msgType;

    @Schema(description = "媒体对象Key")
    private String mediaKey;

    @Schema(description = "视频封面对象Key")
    private String mediaPosterKey;

    @Schema(description = "媒体宽度")
    private Integer mediaWidth;

    @Schema(description = "媒体高度")
    private Integer mediaHeight;

    @Schema(description = "媒体大小")
    private Long mediaSize;

    @Schema(description = "媒体 MIME 类型")
    private String mediaMimeType;

    @Schema(description = "回复目标消息ID")
    private Long replyToMessageId;
}

