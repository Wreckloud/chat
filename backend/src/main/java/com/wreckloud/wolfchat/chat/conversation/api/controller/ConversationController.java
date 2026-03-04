package com.wreckloud.wolfchat.chat.conversation.api.controller;

import com.wreckloud.wolfchat.chat.conversation.api.vo.ConversationVO;
import com.wreckloud.wolfchat.chat.conversation.application.service.ConversationService;
import com.wreckloud.wolfchat.common.security.context.UserContext;
import com.wreckloud.wolfchat.common.web.Result;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

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

    @Operation(summary = "获取或创建会话", description = "与指定行者创建会话，仅互相关注可创建")
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
}

