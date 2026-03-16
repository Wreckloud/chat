package com.wreckloud.wolfchat.admin.api.controller;

import com.wreckloud.wolfchat.admin.application.service.AdminPermissionService;
import com.wreckloud.wolfchat.admin.application.service.AdminUserManageService;
import com.wreckloud.wolfchat.common.security.context.UserContext;
import com.wreckloud.wolfchat.common.web.Result;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * 管理端封禁管理接口。
 */
@Tag(name = "管理端-封禁")
@RestController
@RequestMapping("/admin/bans")
@RequiredArgsConstructor
public class AdminBanController {
    private final AdminUserManageService adminUserManageService;
    private final AdminPermissionService adminPermissionService;

    @Operation(summary = "解除封禁")
    @PutMapping("/{banId}/lift")
    public Result<Void> liftBan(@PathVariable Long banId) {
        adminPermissionService.assertAdmin(UserContext.getRequiredUserId());
        adminUserManageService.liftUserBan(banId);
        return Result.success("解除封禁成功", null);
    }
}
