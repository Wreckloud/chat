package com.wreckloud.wolfchat.notice.api.controller;

import com.wreckloud.wolfchat.common.security.context.UserContext;
import com.wreckloud.wolfchat.common.web.Result;
import com.wreckloud.wolfchat.notice.api.vo.UserNoticePageVO;
import com.wreckloud.wolfchat.notice.application.service.UserNoticeService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

/**
 * @Description 系统通知 Controller
 * @Author Wreckloud
 * @Date 2026-03-16
 */
@Tag(name = "系统通知", description = "系统通知相关接口")
@RestController
@RequestMapping("/notices")
@RequiredArgsConstructor
public class NoticeController {
    private final UserNoticeService userNoticeService;

    @Operation(summary = "通知列表", description = "获取当前用户的系统通知列表")
    @GetMapping
    public Result<UserNoticePageVO> listNotices(@RequestParam(defaultValue = "1") long page,
                                                 @RequestParam(defaultValue = "20") long size) {
        Long userId = UserContext.getRequiredUserId();
        return Result.success(userNoticeService.listNotices(userId, page, size));
    }

    @Operation(summary = "未读通知数", description = "获取当前用户的系统通知未读数量")
    @GetMapping("/unread-count")
    public Result<Long> countUnread() {
        Long userId = UserContext.getRequiredUserId();
        return Result.success(userNoticeService.countUnread(userId));
    }

    @Operation(summary = "标记通知已读", description = "将指定通知标记为已读")
    @PutMapping("/{noticeId}/read")
    public Result<Void> markRead(@PathVariable Long noticeId) {
        Long userId = UserContext.getRequiredUserId();
        userNoticeService.markRead(userId, noticeId);
        return Result.success(null);
    }

    @Operation(summary = "全部标记已读", description = "将当前用户全部通知标记为已读")
    @PutMapping("/read-all")
    public Result<Void> markAllRead() {
        Long userId = UserContext.getRequiredUserId();
        userNoticeService.markAllRead(userId);
        return Result.success(null);
    }
}
