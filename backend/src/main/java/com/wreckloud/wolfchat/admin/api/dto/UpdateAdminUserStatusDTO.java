package com.wreckloud.wolfchat.admin.api.dto;

import com.wreckloud.wolfchat.account.domain.enums.UserStatus;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import javax.validation.constraints.NotNull;

/**
 * 管理端更新用户状态请求。
 */
@Data
@Schema(description = "管理端更新用户状态请求")
public class UpdateAdminUserStatusDTO {
    @NotNull(message = "状态不能为空")
    @Schema(description = "用户状态：NORMAL/DISABLED", required = true)
    private UserStatus status;
}

