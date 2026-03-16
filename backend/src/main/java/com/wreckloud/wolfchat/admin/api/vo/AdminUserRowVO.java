package com.wreckloud.wolfchat.admin.api.vo;

import com.wreckloud.wolfchat.account.domain.enums.UserStatus;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.time.LocalDateTime;

/**
 * 管理端用户列表行。
 */
@Data
@Schema(description = "管理端用户列表行")
public class AdminUserRowVO {
    @Schema(description = "用户ID")
    private Long userId;

    @Schema(description = "狼藉号")
    private String wolfNo;

    @Schema(description = "昵称")
    private String nickname;

    @Schema(description = "状态")
    private UserStatus status;

    @Schema(description = "活跃天数")
    private Integer activeDayCount;

    @Schema(description = "最近登录时间")
    private LocalDateTime lastLoginAt;
}

