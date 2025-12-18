package com.wreckloud.wolfchat.admin.api.controller;

import com.wreckloud.wolfchat.admin.api.vo.AdminStatisticsVO;
import com.wreckloud.wolfchat.admin.application.service.AdminStatisticsService;
import com.wreckloud.wolfchat.common.web.Result;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * @Description 管理员-统计数据API
 * @Author Wreckloud
 * @Date 2024-12-18
 */
@Slf4j
@RestController
@RequestMapping("/admin/statistics")
@RequiredArgsConstructor
@Tag(name = "管理员-统计数据", description = "管理员查看系统统计数据")
public class AdminStatisticsController {
    
    private final AdminStatisticsService adminStatisticsService;
    
    @GetMapping
    @Operation(summary = "获取系统统计数据", description = "获取用户、群组等统计信息")
    public Result<AdminStatisticsVO> getStatistics() {
        AdminStatisticsVO result = adminStatisticsService.getStatistics();
        return Result.ok(result);
    }
}

