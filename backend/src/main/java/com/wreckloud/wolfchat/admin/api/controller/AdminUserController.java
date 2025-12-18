package com.wreckloud.wolfchat.admin.api.controller;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.wreckloud.wolfchat.admin.api.dto.UserStatusUpdateDTO;
import com.wreckloud.wolfchat.admin.api.vo.AdminUserVO;
import com.wreckloud.wolfchat.admin.application.service.AdminUserService;
import com.wreckloud.wolfchat.common.security.context.UserContext;
import com.wreckloud.wolfchat.common.web.Result;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

/**
 * @Description 管理员-用户管理API
 * @Author Wreckloud
 * @Date 2024-12-18
 */
@Slf4j
@RestController
@RequestMapping("/admin/users")
@Tag(name = "管理员-用户管理", description = "管理员对用户的管理操作")
public class AdminUserController {
    
    @Autowired
    private AdminUserService adminUserService;
    
    @GetMapping("/list")
    @Operation(summary = "分页查询用户列表", description = "管理员查询所有用户列表，支持关键词搜索和状态筛选")
    public Result<Page<AdminUserVO>> getUserList(
            @Parameter(description = "当前页", required = true) @RequestParam(defaultValue = "1") Long current,
            @Parameter(description = "每页大小", required = true) @RequestParam(defaultValue = "10") Long size,
            @Parameter(description = "搜索关键词（用户名/手机号/WF号）") @RequestParam(required = false) String keyword,
            @Parameter(description = "状态筛选：1正常 2禁用 3注销") @RequestParam(required = false) Integer status
    ) {
        Page<AdminUserVO> result = adminUserService.getUserList(current, size, keyword, status);
        return Result.ok(result);
    }
    
    @GetMapping("/{userId}")
    @Operation(summary = "查询用户详情", description = "管理员查询指定用户的详细信息")
    public Result<AdminUserVO> getUserDetail(
            @Parameter(description = "用户ID", required = true) @PathVariable Long userId
    ) {
        AdminUserVO result = adminUserService.getUserDetail(userId);
        return Result.ok(result);
    }
    
    @PutMapping("/status")
    @Operation(summary = "更新用户状态", description = "管理员禁用/启用/注销用户")
    public Result<Void> updateUserStatus(@Validated @RequestBody UserStatusUpdateDTO dto) {
        Long adminId = UserContext.getUserId();
        adminUserService.updateUserStatus(dto, adminId);
        return Result.ok();
    }
}

