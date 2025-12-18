package com.wreckloud.wolfchat.admin.api.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

/**
 * @Description 管理员操作日志VO
 * @Author Wreckloud
 * @Date 2024-12-18
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Schema(description = "管理员操作日志")
public class AdminLogVO {
    
    @Schema(description = "日志ID")
    private Long id;
    
    @Schema(description = "管理员用户ID")
    private Long adminId;
    
    @Schema(description = "管理员用户名")
    private String adminName;
    
    @Schema(description = "管理员WF号")
    private Long adminWfNo;
    
    @Schema(description = "操作类型")
    private String action;
    
    @Schema(description = "目标类型")
    private String targetType;
    
    @Schema(description = "目标ID")
    private Long targetId;
    
    @Schema(description = "目标名称")
    private String targetName;
    
    @Schema(description = "操作详情")
    private String details;
    
    @Schema(description = "IP地址")
    private String ipAddress;
    
    @Schema(description = "操作结果：1成功 0失败")
    private Integer result;
    
    @Schema(description = "错误信息")
    private String errorMessage;
    
    @Schema(description = "操作时间")
    private LocalDateTime createTime;
}

