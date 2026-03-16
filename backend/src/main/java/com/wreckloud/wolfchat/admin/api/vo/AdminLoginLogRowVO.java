package com.wreckloud.wolfchat.admin.api.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.time.LocalDateTime;

/**
 * 管理端登录日志列表行。
 */
@Data
@Schema(description = "管理端登录日志列表行")
public class AdminLoginLogRowVO {
    @Schema(description = "日志ID")
    private Long logId;

    @Schema(description = "账号（脱敏）")
    private String accountMask;

    @Schema(description = "登录方式")
    private String loginMethod;

    @Schema(description = "登录结果")
    private String loginResult;

    @Schema(description = "客户端IP")
    private String ip;

    @Schema(description = "客户端类型")
    private String clientType;

    @Schema(description = "登录时间")
    private LocalDateTime loginTime;
}

