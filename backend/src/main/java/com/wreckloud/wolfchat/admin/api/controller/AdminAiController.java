package com.wreckloud.wolfchat.admin.api.controller;

import com.wreckloud.wolfchat.admin.api.dto.AdminAiRuntimeConfigDTO;
import com.wreckloud.wolfchat.admin.api.vo.AdminAiRuntimeConfigVO;
import com.wreckloud.wolfchat.admin.application.service.AdminAiManageService;
import com.wreckloud.wolfchat.admin.application.service.AdminPermissionService;
import com.wreckloud.wolfchat.common.security.context.UserContext;
import com.wreckloud.wolfchat.common.web.Result;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * 管理端 AI 管控接口。
 */
@Tag(name = "管理端-AI管控")
@RestController
@RequestMapping("/admin/ai")
@RequiredArgsConstructor
public class AdminAiController {
    private final AdminPermissionService adminPermissionService;
    private final AdminAiManageService adminAiManageService;

    @Operation(summary = "读取AI运行配置")
    @GetMapping("/config")
    public Result<AdminAiRuntimeConfigVO> getConfig() {
        adminPermissionService.assertAdmin(UserContext.getRequiredUserId());
        return Result.success(adminAiManageService.getRuntimeConfig());
    }

    @Operation(summary = "更新AI运行配置")
    @PutMapping("/config")
    public Result<AdminAiRuntimeConfigVO> updateConfig(@RequestBody @Validated AdminAiRuntimeConfigDTO dto) {
        adminPermissionService.assertAdmin(UserContext.getRequiredUserId());
        return Result.success("保存成功", adminAiManageService.updateRuntimeConfig(dto));
    }
}

