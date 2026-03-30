package com.wreckloud.wolfchat.admin.api.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.util.List;

/**
 * 控制台趋势图数据。
 */
@Data
@Schema(description = "控制台趋势图数据")
public class AdminDashboardTrendVO {
    @Schema(description = "日期标签列表（MM-dd）")
    private List<String> dateLabels;

    @Schema(description = "每日新增用户数")
    private List<Long> registerCounts;

    @Schema(description = "每日登录成功次数")
    private List<Long> loginSuccessCounts;

    @Schema(description = "每日登录失败次数")
    private List<Long> loginFailCounts;

    @Schema(description = "每日发帖数（排除草稿）")
    private List<Long> threadCounts;

    @Schema(description = "每日回帖数")
    private List<Long> replyCounts;

    @Schema(description = "每日聊天室消息数")
    private List<Long> lobbyMessageCounts;

    @Schema(description = "近7日登录客户端分布")
    private List<AdminDashboardClientTypeVO> loginClientTypeDistribution;
}

