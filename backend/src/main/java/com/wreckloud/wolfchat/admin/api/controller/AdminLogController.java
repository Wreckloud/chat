package com.wreckloud.wolfchat.admin.api.controller;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.wreckloud.wolfchat.admin.api.vo.AdminLogVO;
import com.wreckloud.wolfchat.admin.application.service.AdminLogService;
import com.wreckloud.wolfchat.common.web.Result;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

/**
 * @Description 管理员-操作日志API
 * @Author Wreckloud
 * @Date 2024-12-18
 */
@Slf4j
@RestController
@RequestMapping("/admin/logs")
@Tag(name = "管理员-操作日志", description = "管理员查看操作日志")
public class AdminLogController {
    
    @Autowired
    private AdminLogService adminLogService;
    
    @GetMapping("/list")
    @Operation(summary = "分页查询操作日志", description = "管理员查询操作日志，支持管理员和操作类型筛选")
    public Result<Page<AdminLogVO>> getLogList(
            @Parameter(description = "当前页", required = true) @RequestParam(defaultValue = "1") Long current,
            @Parameter(description = "每页大小", required = true) @RequestParam(defaultValue = "10") Long size,
            @Parameter(description = "管理员ID筛选") @RequestParam(required = false) Long adminId,
            @Parameter(description = "操作类型筛选") @RequestParam(required = false) String action
    ) {
        Page<AdminLogVO> result = adminLogService.getLogList(current, size, adminId, action);
        return Result.ok(result);
    }
}

