package com.wreckloud.wolfchat.admin.api.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

/**
 * 控制台登录客户端分布项。
 */
@Data
@Schema(description = "控制台登录客户端分布项")
public class AdminDashboardClientTypeVO {
    @Schema(description = "客户端类型")
    private String clientType;

    @Schema(description = "次数")
    private Long count;
}

