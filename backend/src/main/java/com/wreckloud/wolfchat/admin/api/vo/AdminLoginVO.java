package com.wreckloud.wolfchat.admin.api.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

/**
 * 管理端登录响应。
 */
@Data
@Schema(description = "管理端登录响应")
public class AdminLoginVO {
    @Schema(description = "JWT token")
    private String token;
}

