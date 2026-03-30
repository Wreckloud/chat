package com.wreckloud.wolfchat.admin.api.controller;

import com.wreckloud.wolfchat.admin.api.vo.AdminDashboardOverviewVO;
import com.wreckloud.wolfchat.admin.api.vo.AdminDashboardTrendVO;
import com.wreckloud.wolfchat.admin.application.service.AdminDashboardService;
import com.wreckloud.wolfchat.admin.application.service.AdminPermissionService;
import com.wreckloud.wolfchat.common.security.context.UserContext;
import com.wreckloud.wolfchat.common.web.Result;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

/**
 * 管理端控制台接口。
 */
@Tag(name = "管理端-控制台")
@RestController
@RequestMapping("/admin/dashboard")
@RequiredArgsConstructor
public class AdminDashboardController {
    private final AdminDashboardService adminDashboardService;
    private final AdminPermissionService adminPermissionService;

    @Operation(summary = "控制台概览")
    @GetMapping("/overview")
    public Result<AdminDashboardOverviewVO> getOverview() {
        adminPermissionService.assertAdmin(UserContext.getRequiredUserId());
        return Result.success(adminDashboardService.getOverview());
    }

    @Operation(summary = "控制台趋势")
    @GetMapping("/trend")
    public Result<AdminDashboardTrendVO> getTrend(@RequestParam(defaultValue = "7") int days) {
        adminPermissionService.assertAdmin(UserContext.getRequiredUserId());
        return Result.success(adminDashboardService.getTrend(days));
    }
}
