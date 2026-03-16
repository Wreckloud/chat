package com.wreckloud.wolfchat.admin.api.controller;

import com.wreckloud.wolfchat.admin.api.dto.AdminLoginDTO;
import com.wreckloud.wolfchat.admin.api.vo.AdminLoginVO;
import com.wreckloud.wolfchat.admin.api.vo.AdminProfileVO;
import com.wreckloud.wolfchat.admin.application.service.AdminAuthService;
import com.wreckloud.wolfchat.admin.application.service.AdminPermissionService;
import com.wreckloud.wolfchat.common.excption.BaseException;
import com.wreckloud.wolfchat.common.excption.ErrorCode;
import com.wreckloud.wolfchat.common.security.context.UserContext;
import com.wreckloud.wolfchat.common.web.Result;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import javax.servlet.http.HttpServletRequest;

/**
 * 管理端认证接口。
 */
@Slf4j
@Tag(name = "管理端-认证")
@RestController
@RequestMapping("/admin/auth")
@RequiredArgsConstructor
public class AdminAuthController {
    private final AdminAuthService adminAuthService;
    private final AdminPermissionService adminPermissionService;

    @Operation(summary = "管理端登录")
    @PostMapping("/login")
    public Result<AdminLoginVO> login(@RequestBody @Validated AdminLoginDTO dto, HttpServletRequest request) {
        try {
            AdminLoginVO loginVO = adminAuthService.login(dto.getAccount(), dto.getPassword(), request);
            return Result.success("登录成功", loginVO);
        } catch (BaseException e) {
            log.debug("管理端登录失败: code={}", e.getCode());
            return Result.error(ErrorCode.LOGIN_FAILED);
        } catch (Exception e) {
            log.error("管理端登录异常", e);
            return Result.error(ErrorCode.LOGIN_FAILED);
        }
    }

    @Operation(summary = "管理端当前登录信息")
    @GetMapping("/me")
    public Result<AdminProfileVO> me() {
        Long userId = UserContext.getRequiredUserId();
        adminPermissionService.assertAdmin(userId);
        AdminProfileVO profileVO = adminAuthService.getCurrentAdminProfile(userId);
        return Result.success(profileVO);
    }
}
