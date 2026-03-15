package com.wreckloud.wolfchat.chat.lobby.api.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.time.LocalDateTime;
import java.util.List;

/**
 * @Description 大厅元信息 VO
 * @Author Wreckloud
 * @Date 2026-03-12
 */
@Data
@Schema(description = "大厅元信息")
public class LobbyMetaVO {
    @Schema(description = "在线人数")
    private Integer onlineCount;

    @Schema(description = "最近活跃时间")
    private LocalDateTime latestActiveAt;

    @Schema(description = "最近在线用户列表")
    private List<LobbyRecentUserVO> recentUsers;
}

