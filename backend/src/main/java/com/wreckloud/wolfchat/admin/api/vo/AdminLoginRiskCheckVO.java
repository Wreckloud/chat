package com.wreckloud.wolfchat.admin.api.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

/**
 * 管理端登录风控单项检测结果。
 */
@Data
@Schema(description = "管理端登录风控单项检测结果")
public class AdminLoginRiskCheckVO {
    @Schema(description = "检测账号")
    private String account;

    @Schema(description = "检测IP")
    private String ip;

    @Schema(description = "账号是否被锁定")
    private Boolean accountLocked;

    @Schema(description = "账号锁定剩余秒数")
    private Long accountLockTtlSeconds;

    @Schema(description = "账号失败次数（窗口内）")
    private Long accountFailCount;

    @Schema(description = "IP是否被锁定")
    private Boolean ipLocked;

    @Schema(description = "IP锁定剩余秒数")
    private Long ipLockTtlSeconds;

    @Schema(description = "IP失败次数（窗口内）")
    private Long ipFailCount;
}

