package com.wreckloud.wolfchat.chat.message.api.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

/**
 * 会话消息策略 VO
 */
@Data
@Schema(description = "会话消息策略")
public class MessagePolicyVO {
    @Schema(description = "会话ID")
    private Long conversationId;

    @Schema(description = "当前是否互关")
    private Boolean mutualFollow;

    @Schema(description = "是否已互动（任意一方收到过对方消息）")
    private Boolean interactionUnlocked;

    @Schema(description = "是否可自由发送")
    private Boolean canSendFreely;

    @Schema(description = "未互关单向发送上限")
    private Integer strangerMessageLimit;

    @Schema(description = "当前用户已发送条数（该会话）")
    private Integer strangerMessageSent;

    @Schema(description = "未互关可用剩余条数")
    private Integer strangerMessageRemaining;
}
