package com.wreckloud.wolfchat.admin.api.vo;

import com.wreckloud.wolfchat.chat.message.domain.enums.MessageType;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.time.LocalDateTime;

/**
 * 管理端大厅消息治理行数据。
 */
@Data
@Schema(description = "管理端大厅消息治理行")
public class AdminLobbyMessageRowVO {
    @Schema(description = "消息ID")
    private Long messageId;

    @Schema(description = "发送者昵称")
    private String senderNickname;

    @Schema(description = "消息类型")
    private MessageType msgType;

    @Schema(description = "消息内容")
    private String content;

    @Schema(description = "媒体对象Key")
    private String mediaKey;

    @Schema(description = "是否已撤回")
    private Boolean recalled;

    @Schema(description = "发送时间")
    private LocalDateTime createTime;
}

