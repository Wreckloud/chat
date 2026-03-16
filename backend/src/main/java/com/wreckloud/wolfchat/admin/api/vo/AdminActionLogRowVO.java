package com.wreckloud.wolfchat.admin.api.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.time.LocalDateTime;

/**
 * 管理端操作日志列表行。
 */
@Data
@Schema(description = "管理端操作日志列表行")
public class AdminActionLogRowVO {
    @Schema(description = "日志ID")
    private Long actionLogId;

    @Schema(description = "操作人")
    private String operatorName;

    @Schema(description = "操作类型")
    private String actionType;

    @Schema(description = "目标类型")
    private String targetType;

    @Schema(description = "目标ID")
    private Long targetId;

    @Schema(description = "详情")
    private String detail;

    @Schema(description = "操作时间")
    private LocalDateTime createTime;
}

