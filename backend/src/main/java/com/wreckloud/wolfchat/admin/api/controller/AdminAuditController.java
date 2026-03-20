package com.wreckloud.wolfchat.admin.api.controller;

import com.wreckloud.wolfchat.admin.api.vo.AdminActionLogRowVO;
import com.wreckloud.wolfchat.admin.api.vo.AdminLoginLogRowVO;
import com.wreckloud.wolfchat.admin.api.vo.AdminLoginRiskCheckVO;
import com.wreckloud.wolfchat.admin.api.vo.AdminLoginRiskOverviewVO;
import com.wreckloud.wolfchat.admin.api.vo.AdminPageVO;
import com.wreckloud.wolfchat.admin.application.service.AdminAuditService;
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
 * 管理端审计接口。
 */
@Tag(name = "管理端-审计")
@RestController
@RequestMapping("/admin/audit")
@RequiredArgsConstructor
public class AdminAuditController {
    private final AdminAuditService adminAuditService;
    private final AdminPermissionService adminPermissionService;

    @Operation(summary = "操作日志分页列表")
    @GetMapping("/actions")
    public Result<AdminPageVO<AdminActionLogRowVO>> listActionLogs(
            @RequestParam(defaultValue = "1") long page,
            @RequestParam(defaultValue = "20") long size
    ) {
        adminPermissionService.assertAdmin(UserContext.getRequiredUserId());
        return Result.success(adminAuditService.listActionLogPage(page, size));
    }

    @Operation(summary = "登录日志分页列表")
    @GetMapping("/logins")
    public Result<AdminPageVO<AdminLoginLogRowVO>> listLoginLogs(
            @RequestParam(defaultValue = "1") long page,
            @RequestParam(defaultValue = "20") long size
    ) {
        adminPermissionService.assertAdmin(UserContext.getRequiredUserId());
        return Result.success(adminAuditService.listLoginLogPage(page, size));
    }

    @Operation(summary = "登录风控总览")
    @GetMapping("/login-risk/overview")
    public Result<AdminLoginRiskOverviewVO> getLoginRiskOverview() {
        adminPermissionService.assertAdmin(UserContext.getRequiredUserId());
        return Result.success(adminAuditService.getLoginRiskOverview());
    }

    @Operation(summary = "登录风控单项检测")
    @GetMapping("/login-risk/check")
    public Result<AdminLoginRiskCheckVO> checkLoginRisk(
            @RequestParam(defaultValue = "") String account,
            @RequestParam(defaultValue = "") String ip
    ) {
        adminPermissionService.assertAdmin(UserContext.getRequiredUserId());
        return Result.success(adminAuditService.checkLoginRisk(account, ip));
    }
}
