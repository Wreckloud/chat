package com.wreckloud.wolfchat.account.api.controller;

import com.wreckloud.wolfchat.account.api.dto.ChangePasswordDTO;
import com.wreckloud.wolfchat.account.api.vo.UserVO;
import com.wreckloud.wolfchat.account.application.service.AuthService;
import com.wreckloud.wolfchat.account.application.service.UserService;
import com.wreckloud.wolfchat.common.security.context.UserContext;
import com.wreckloud.wolfchat.common.web.Result;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * @Description 行者 Controller
 * @Author Wreckloud
 * @Date 2026-01-06
 */
@Tag(name = "行者", description = "行者信息相关接口")
@RestController
@RequestMapping("/users")
@RequiredArgsConstructor
public class UserController {
    private final UserService userService;
    private final AuthService authService;

    /**
     * 获取当前登录行者信息
     */
    @Operation(summary = "获取当前行者信息", description = "获取当前登录行者的信息（需要登录）")
    @GetMapping("/me")
    public Result<UserVO> getCurrentUser() {
        Long userId = UserContext.getRequiredUserId();
        return Result.success(userService.getUserVOById(userId));
    }

    /**
     * 获取指定行者信息
     */
    @Operation(summary = "获取指定行者信息", description = "根据行者ID获取基础信息（需要登录）")
    @GetMapping("/{userId}")
    public Result<UserVO> getUserById(@PathVariable Long userId) {
        return Result.success(userService.getUserVOById(userId));
    }

    /**
     * 修改当前登录行者的密码
     */
    @Operation(summary = "修改密码", description = "校验原密码后修改当前登录行者密码（需要登录）")
    @PutMapping("/password")
    public Result<Void> changePassword(@RequestBody @Validated ChangePasswordDTO dto) {
        Long userId = UserContext.getRequiredUserId();
        authService.changePassword(userId, dto.getOldLoginKey(), dto.getNewLoginKey(), dto.getConfirmLoginKey());
        return Result.success("密码修改成功", null);
    }
}


