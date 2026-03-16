package com.wreckloud.wolfchat.admin.api.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

/**
 * 管理端控制台概览数据。
 */
@Data
@Schema(description = "管理端控制台概览")
public class AdminDashboardOverviewVO {
    @Schema(description = "用户总数")
    private Long userCount;

    @Schema(description = "近7日活跃用户数")
    private Long activeUser7d;

    @Schema(description = "主题总数")
    private Long threadCount;

    @Schema(description = "回复总数")
    private Long replyCount;

    @Schema(description = "当前在线人数")
    private Integer onlineUserCount;

    @Schema(description = "近24小时登录失败次数")
    private Long loginFail24h;

    @Schema(description = "今日发帖数")
    private Long todayThreadCount;

    @Schema(description = "今日回帖数")
    private Long todayReplyCount;
}

