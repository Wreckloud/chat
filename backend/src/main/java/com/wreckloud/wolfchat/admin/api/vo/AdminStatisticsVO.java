package com.wreckloud.wolfchat.admin.api.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * @Description 管理员统计数据VO
 * @Author Wreckloud
 * @Date 2024-12-18
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Schema(description = "管理员统计数据")
public class AdminStatisticsVO {
    
    @Schema(description = "总用户数")
    private Long totalUsers;
    
    @Schema(description = "今日新增用户数")
    private Long todayNewUsers;
    
    @Schema(description = "活跃用户数（7天内登录）")
    private Long activeUsers;
    
    @Schema(description = "禁用用户数")
    private Long disabledUsers;
    
    @Schema(description = "总群组数")
    private Long totalGroups;
    
    @Schema(description = "今日新增群组数")
    private Long todayNewGroups;
    
    @Schema(description = "活跃群组数（7天内有消息）")
    private Long activeGroups;
    
    @Schema(description = "已解散群组数")
    private Long disbandedGroups;
}

