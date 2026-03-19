package com.wreckloud.wolfchat.chat.conversation.api.controller;

import com.wreckloud.wolfchat.chat.conversation.api.vo.ConversationVO;
import com.wreckloud.wolfchat.chat.conversation.application.service.ConversationService;
import com.wreckloud.wolfchat.common.security.context.UserContext;
import com.wreckloud.wolfchat.common.web.Result;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

/**
 * @Description 会话控制器
 * @Author Wreckloud
 * @Date 2026-01-29
 */
@Tag(name = "聊天-会话", description = "会话管理相关接口")
@RestController
@RequestMapping("/conversations")
@RequiredArgsConstructor
public class ConversationController {
    private final ConversationService conversationService;

    @Operation(summary = "获取或创建会话", description = "与指定行者创建会话")
    @PostMapping("/{targetUserId}")
    public Result<Long> getOrCreateConversation(@PathVariable Long targetUserId) {
        Long userId = UserContext.getRequiredUserId();
        Long conversationId = conversationService.getOrCreateConversation(userId, targetUserId);
        return Result.success(conversationId);
    }

    @Operation(summary = "获取会话列表", description = "获取当前行者的所有会话列表")
    @GetMapping
    public Result<List<ConversationVO>> listConversations() {
        Long userId = UserContext.getRequiredUserId();
        return Result.success(conversationService.listConversationVOs(userId));
    }

    @Operation(summary = "获取会话未读总数", description = "获取当前行者所有会话的未读总数")
    @GetMapping("/unread-count")
    public Result<Long> countUnread() {
        Long userId = UserContext.getRequiredUserId();
        return Result.success(conversationService.getUnreadTotal(userId));
    }

    @Operation(summary = "标记会话已读", description = "将当前行者在该会话中的未读数清零")
    @PutMapping("/{conversationId}/read")
    public Result<Void> markConversationRead(@PathVariable Long conversationId) {
        Long userId = UserContext.getRequiredUserId();
        conversationService.markConversationRead(conversationId, userId);
        return Result.success(null);
    }

    @Operation(summary = "标记会话未读", description = "将当前行者在该会话中的未读数标记为1")
    @PutMapping("/{conversationId}/unread")
    public Result<Void> markConversationUnread(@PathVariable Long conversationId) {
        Long userId = UserContext.getRequiredUserId();
        conversationService.markConversationUnread(conversationId, userId);
        return Result.success(null);
    }
}

