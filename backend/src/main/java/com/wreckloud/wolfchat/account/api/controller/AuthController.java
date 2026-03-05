package com.wreckloud.wolfchat.account.api.controller;

import com.wreckloud.wolfchat.account.api.dto.LoginDTO;
import com.wreckloud.wolfchat.account.api.dto.RegisterDTO;
import com.wreckloud.wolfchat.account.api.dto.ResetPasswordByEmailDTO;
import com.wreckloud.wolfchat.account.api.dto.SendEmailCodeDTO;
import com.wreckloud.wolfchat.account.api.vo.LoginVO;
import com.wreckloud.wolfchat.account.application.service.AuthService;
import com.wreckloud.wolfchat.common.web.Result;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import javax.servlet.http.HttpServletRequest;

/**
 * @Description 认证控制器
 * @Author Wreckloud
 * @Date 2026-01-06
 */
@Tag(name = "认证", description = "认证相关接口")
@RestController
@RequestMapping("/auth")
@RequiredArgsConstructor
public class AuthController {
    private final AuthService authService;

    /**
     * 注册
     * 用户输入行者名、密码、可选邮箱，系统分配狼藉号并注册
     */
    @Operation(summary = "注册", description = "输入行者名、密码、可选邮箱，系统分配狼藉号并注册")
    @PostMapping("/register")
    public Result<LoginVO> register(@RequestBody @Validated RegisterDTO dto) {
        LoginVO loginVO = authService.register(dto.getNickname(), dto.getPassword(), dto.getEmail());
        return Result.success("注册成功", loginVO);
    }

    /**
     * 账号+密码登录（账号支持狼藉号或邮箱）
     */
    @Operation(summary = "登录", description = "使用账号（狼藉号或邮箱）+密码登录")
    @PostMapping("/login")
    public Result<LoginVO> login(@RequestBody @Validated LoginDTO dto, HttpServletRequest request) {
        LoginVO loginVO = authService.login(dto.getAccount(), dto.getLoginKey(), request);
        return Result.success("登录成功", loginVO);
    }

    /**
     * 发送重置密码邮箱验证码
     */
    @Operation(summary = "发送重置密码验证码", description = "发送重置密码邮箱验证码")
    @PostMapping("/password/reset-code/send")
    public Result<Void> sendResetPasswordCode(@RequestBody @Validated SendEmailCodeDTO dto) {
        authService.sendResetPasswordCode(dto.getEmail());
        return Result.success("验证码已发送", null);
    }

    /**
     * 通过邮箱验证码重置密码
     */
    @Operation(summary = "邮箱重置密码", description = "通过邮箱验证码重置密码")
    @PutMapping("/password/reset-by-email")
    public Result<Void> resetPasswordByEmail(@RequestBody @Validated ResetPasswordByEmailDTO dto) {
        authService.resetPasswordByEmail(dto.getEmail(), dto.getVerifyCode(), dto.getNewLoginKey(), dto.getConfirmLoginKey());
        return Result.success("密码重置成功", null);
    }
}

