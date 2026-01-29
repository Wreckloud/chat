package com.wreckloud.wolfchat.account.api.controller;

import com.wreckloud.wolfchat.account.api.dto.LoginDTO;
import com.wreckloud.wolfchat.account.api.dto.RegisterDTO;
import com.wreckloud.wolfchat.account.api.vo.LoginVO;
import com.wreckloud.wolfchat.account.application.service.AuthService;
import com.wreckloud.wolfchat.common.web.Result;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

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
     * 用户输入行者名和密码，系统分配狼藉号并注册
     */
    @Operation(summary = "注册", description = "输入行者名和密码，系统分配狼藉号并注册")
    @PostMapping("/register")
    public Result<LoginVO> register(@RequestBody @Validated RegisterDTO dto) {
        LoginVO loginVO = authService.register(dto.getNickname(), dto.getPassword());
        return Result.success("注册成功", loginVO);
    }

    /**
     * 狼藉号+密码登录
     */
    @Operation(summary = "登录", description = "使用狼藉号+密码登录")
    @PostMapping("/login")
    public Result<LoginVO> login(@RequestBody @Validated LoginDTO dto) {
        LoginVO loginVO = authService.login(dto.getWolfNo(), dto.getLoginKey());
        return Result.success("登录成功", loginVO);
    }
}

