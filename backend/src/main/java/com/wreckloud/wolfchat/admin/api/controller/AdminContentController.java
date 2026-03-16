package com.wreckloud.wolfchat.admin.api.controller;

import com.wreckloud.wolfchat.admin.api.vo.AdminPageVO;
import com.wreckloud.wolfchat.admin.api.vo.AdminReplyRowVO;
import com.wreckloud.wolfchat.admin.api.vo.AdminThreadRowVO;
import com.wreckloud.wolfchat.admin.application.service.AdminContentManageService;
import com.wreckloud.wolfchat.admin.application.service.AdminPermissionService;
import com.wreckloud.wolfchat.common.security.context.UserContext;
import com.wreckloud.wolfchat.common.web.Result;
import com.wreckloud.wolfchat.community.api.dto.UpdateThreadEssenceDTO;
import com.wreckloud.wolfchat.community.api.dto.UpdateThreadLockDTO;
import com.wreckloud.wolfchat.community.api.dto.UpdateThreadStickyDTO;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

/**
 * 管理端内容治理接口。
 */
@Tag(name = "管理端-内容治理")
@RestController
@RequestMapping("/admin/forum")
@RequiredArgsConstructor
public class AdminContentController {
    private final AdminContentManageService adminContentManageService;
    private final AdminPermissionService adminPermissionService;

    @Operation(summary = "主题分页列表")
    @GetMapping("/threads")
    public Result<AdminPageVO<AdminThreadRowVO>> listThreads(
            @RequestParam(defaultValue = "1") long page,
            @RequestParam(defaultValue = "20") long size
    ) {
        adminPermissionService.assertAdmin(UserContext.getRequiredUserId());
        return Result.success(adminContentManageService.listThreadPage(page, size));
    }

    @Operation(summary = "回复分页列表")
    @GetMapping("/replies")
    public Result<AdminPageVO<AdminReplyRowVO>> listReplies(
            @RequestParam(defaultValue = "1") long page,
            @RequestParam(defaultValue = "20") long size
    ) {
        adminPermissionService.assertAdmin(UserContext.getRequiredUserId());
        return Result.success(adminContentManageService.listReplyPage(page, size));
    }

    @Operation(summary = "更新主题锁定状态")
    @PutMapping("/threads/{threadId}/lock")
    public Result<Void> updateThreadLock(@PathVariable Long threadId, @RequestBody @Validated UpdateThreadLockDTO dto) {
        Long operatorUserId = UserContext.getRequiredUserId();
        adminPermissionService.assertAdmin(operatorUserId);
        adminContentManageService.updateThreadLockStatus(operatorUserId, threadId, Boolean.TRUE.equals(dto.getLocked()));
        return Result.success("操作成功", null);
    }

    @Operation(summary = "更新主题置顶状态")
    @PutMapping("/threads/{threadId}/sticky")
    public Result<Void> updateThreadSticky(@PathVariable Long threadId, @RequestBody @Validated UpdateThreadStickyDTO dto) {
        Long operatorUserId = UserContext.getRequiredUserId();
        adminPermissionService.assertAdmin(operatorUserId);
        adminContentManageService.updateThreadStickyStatus(operatorUserId, threadId, Boolean.TRUE.equals(dto.getSticky()));
        return Result.success("操作成功", null);
    }

    @Operation(summary = "更新主题精华状态")
    @PutMapping("/threads/{threadId}/essence")
    public Result<Void> updateThreadEssence(@PathVariable Long threadId, @RequestBody @Validated UpdateThreadEssenceDTO dto) {
        Long operatorUserId = UserContext.getRequiredUserId();
        adminPermissionService.assertAdmin(operatorUserId);
        adminContentManageService.updateThreadEssenceStatus(operatorUserId, threadId, Boolean.TRUE.equals(dto.getEssence()));
        return Result.success("操作成功", null);
    }

    @Operation(summary = "删除主题")
    @DeleteMapping("/threads/{threadId}")
    public Result<Void> deleteThread(@PathVariable Long threadId) {
        Long operatorUserId = UserContext.getRequiredUserId();
        adminPermissionService.assertAdmin(operatorUserId);
        adminContentManageService.deleteThread(operatorUserId, threadId);
        return Result.success("删除成功", null);
    }

    @Operation(summary = "删除回复")
    @DeleteMapping("/replies/{replyId}")
    public Result<Void> deleteReply(@PathVariable Long replyId) {
        Long operatorUserId = UserContext.getRequiredUserId();
        adminPermissionService.assertAdmin(operatorUserId);
        adminContentManageService.deleteReply(operatorUserId, replyId);
        return Result.success("删除成功", null);
    }
}
