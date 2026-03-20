package com.wreckloud.wolfchat.admin.api.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

/**
 * 管理端登录风控总览。
 */
@Data
@Schema(description = "管理端登录风控总览")
public class AdminLoginRiskOverviewVO {
    @Schema(description = "当前账号锁定数")
    private Long accountLockCount;

    @Schema(description = "当前IP锁定数")
    private Long ipLockCount;

    @Schema(description = "账号失败桶数量（窗口内）")
    private Long accountFailBucketCount;

    @Schema(description = "IP失败桶数量（窗口内）")
    private Long ipFailBucketCount;
}

