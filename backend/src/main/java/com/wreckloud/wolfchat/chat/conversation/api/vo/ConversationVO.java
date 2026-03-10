package com.wreckloud.wolfchat.chat.conversation.api.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.time.LocalDateTime;

/**
 * @Description 会话 VO
 * @Author Wreckloud
 * @Date 2026-01-29
 */
@Data
@Schema(description = "会话信息")
public class ConversationVO {
    @Schema(description = "会话ID")
    private Long conversationId;

    @Schema(description = "对方用户ID")
    private Long targetUserId;

    @Schema(description = "对方狼藉号")
    private String targetWolfNo;

    @Schema(description = "对方行者名")
    private String targetNickname;

    @Schema(description = "对方头像")
    private String targetAvatar;

    @Schema(description = "最近消息内容")
    private String lastMessage;

    @Schema(description = "最近消息时间")
    private LocalDateTime lastMessageTime;

    @Schema(description = "未读消息数")
    private Integer unreadCount;

    @Schema(description = "是否在线")
    private Boolean isOnline;

    @Schema(description = "最近在线时间")
    private LocalDateTime lastSeenAt;
}

