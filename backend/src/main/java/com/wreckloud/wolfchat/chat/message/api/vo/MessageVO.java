package com.wreckloud.wolfchat.chat.message.api.vo;

import com.fasterxml.jackson.annotation.JsonFormat;
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

    @Schema(description = "接收者ID")
    private Long receiverId;

    @Schema(description = "消息内容")
    private String content;

    @Schema(description = "消息类型")
    private MessageType msgType;

    @Schema(description = "发送时间")
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private LocalDateTime createTime;
}

