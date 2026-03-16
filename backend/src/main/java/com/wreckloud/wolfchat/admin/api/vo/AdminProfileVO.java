package com.wreckloud.wolfchat.admin.api.vo;

import com.wreckloud.wolfchat.account.domain.enums.UserStatus;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.time.LocalDateTime;

/**
 * 管理端当前用户信息。
 */
@Data
@Schema(description = "管理端当前用户信息")
public class AdminProfileVO {
    @Schema(description = "用户ID")
    private Long userId;

    @Schema(description = "狼藉号")
    private String wolfNo;

    @Schema(description = "管理端账号名（用于展示）")
    private String username;

    @Schema(description = "昵称")
    private String nickname;

    @Schema(description = "账号状态")
    private UserStatus status;

    @Schema(description = "最近登录时间")
    private LocalDateTime lastLoginAt;
}

