package com.wreckloud.wolfchat.chat.lobby.api.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.time.LocalDateTime;

/**
 * @Description 大厅最近在线用户 VO
 * @Author Wreckloud
 * @Date 2026-03-12
 */
@Data
@Schema(description = "大厅最近在线用户")
public class LobbyRecentUserVO {
    @Schema(description = "用户ID")
    private Long userId;

    @Schema(description = "狼藉号")
    private String wolfNo;

    @Schema(description = "行者名")
    private String nickname;

    @Schema(description = "头像")
    private String avatar;

    @Schema(description = "是否在线")
    private Boolean online;

    @Schema(description = "最近活跃时间")
    private LocalDateTime lastActiveAt;
}

