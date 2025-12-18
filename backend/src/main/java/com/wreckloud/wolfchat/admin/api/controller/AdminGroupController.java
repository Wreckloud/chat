package com.wreckloud.wolfchat.admin.api.controller;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.wreckloud.wolfchat.admin.api.dto.GroupDismissDTO;
import com.wreckloud.wolfchat.admin.api.vo.AdminGroupVO;
import com.wreckloud.wolfchat.admin.application.service.AdminGroupService;
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
 * @Description 管理员-群组管理API
 * @Author Wreckloud
 * @Date 2024-12-18
 */
@Slf4j
@RestController
@RequestMapping("/admin/groups")
@Tag(name = "管理员-群组管理", description = "管理员对群组的管理操作")
public class AdminGroupController {
    
    @Autowired
    private AdminGroupService adminGroupService;
    
    @GetMapping("/list")
    @Operation(summary = "分页查询群组列表", description = "管理员查询所有群组列表，支持关键词搜索和状态筛选")
    public Result<Page<AdminGroupVO>> getGroupList(
            @Parameter(description = "当前页", required = true) @RequestParam(defaultValue = "1") Long current,
            @Parameter(description = "每页大小", required = true) @RequestParam(defaultValue = "10") Long size,
            @Parameter(description = "搜索关键词（群名称）") @RequestParam(required = false) String keyword,
            @Parameter(description = "状态筛选：1正常 2已解散") @RequestParam(required = false) Integer status
    ) {
        Page<AdminGroupVO> result = adminGroupService.getGroupList(current, size, keyword, status);
        return Result.ok(result);
    }
    
    @GetMapping("/{groupId}")
    @Operation(summary = "查询群组详情", description = "管理员查询指定群组的详细信息")
    public Result<AdminGroupVO> getGroupDetail(
            @Parameter(description = "群组ID", required = true) @PathVariable Long groupId
    ) {
        AdminGroupVO result = adminGroupService.getGroupDetail(groupId);
        return Result.ok(result);
    }
    
    @DeleteMapping("/dismiss")
    @Operation(summary = "强制解散群组", description = "管理员强制解散指定群组")
    public Result<Void> dismissGroup(@Validated @RequestBody GroupDismissDTO dto) {
        Long adminId = UserContext.getUserId();
        adminGroupService.dismissGroup(dto, adminId);
        return Result.ok();
    }
}

