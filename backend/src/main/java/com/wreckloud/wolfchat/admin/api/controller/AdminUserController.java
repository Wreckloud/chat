package com.wreckloud.wolfchat.admin.api.controller;

import com.wreckloud.wolfchat.account.domain.enums.UserStatus;
import com.wreckloud.wolfchat.admin.api.dto.CreateUserBanDTO;
import com.wreckloud.wolfchat.admin.api.dto.UpdateAdminUserStatusDTO;
import com.wreckloud.wolfchat.admin.api.vo.AdminPageVO;
import com.wreckloud.wolfchat.admin.api.vo.AdminUserRowVO;
import com.wreckloud.wolfchat.admin.application.service.AdminPermissionService;
import com.wreckloud.wolfchat.admin.application.service.AdminUserManageService;
import com.wreckloud.wolfchat.common.security.context.UserContext;
import com.wreckloud.wolfchat.common.web.Result;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

/**
 * 管理端用户管理接口。
 */
@Tag(name = "管理端-用户")
@RestController
@RequestMapping("/admin/users")
@RequiredArgsConstructor
public class AdminUserController {
    private final AdminUserManageService adminUserManageService;
    private final AdminPermissionService adminPermissionService;

    @Operation(summary = "用户分页列表")
    @GetMapping
    public Result<AdminPageVO<AdminUserRowVO>> listUsers(
            @RequestParam(defaultValue = "1") long page,
            @RequestParam(defaultValue = "20") long size,
            @RequestParam(required = false) String keyword,
            @RequestParam(required = false) UserStatus status
    ) {
        adminPermissionService.assertAdmin(UserContext.getRequiredUserId());
        AdminPageVO<AdminUserRowVO> result = adminUserManageService.listUsers(page, size, keyword, status);
        return Result.success(result);
    }

    @Operation(summary = "更新用户状态")
    @PutMapping("/{userId}/status")
    public Result<Void> updateStatus(@PathVariable Long userId, @RequestBody @Validated UpdateAdminUserStatusDTO dto) {
        adminPermissionService.assertAdmin(UserContext.getRequiredUserId());
        adminUserManageService.updateUserStatus(userId, dto.getStatus());
        return Result.success("状态更新成功", null);
    }

    @Operation(summary = "封禁用户")
    @PostMapping("/{userId}/ban")
    public Result<Void> createBan(@PathVariable Long userId, @RequestBody @Validated CreateUserBanDTO dto) {
        Long operatorUserId = UserContext.getRequiredUserId();
        adminPermissionService.assertAdmin(operatorUserId);
        adminUserManageService.createUserBan(operatorUserId, userId, dto.getReason(), dto.getDurationHours());
        return Result.success("封禁成功", null);
    }
}
