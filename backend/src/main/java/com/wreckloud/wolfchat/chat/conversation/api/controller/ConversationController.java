package com.wreckloud.wolfchat.chat.conversation.api.controller;

import com.wreckloud.wolfchat.account.domain.entity.WfUser;
import com.wreckloud.wolfchat.chat.conversation.api.vo.ConversationVO;
import com.wreckloud.wolfchat.chat.conversation.application.service.ConversationService;
import com.wreckloud.wolfchat.chat.conversation.domain.entity.WfConversation;
import com.wreckloud.wolfchat.common.security.context.UserContext;
import com.wreckloud.wolfchat.common.web.Result;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

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
        Long conversationId = conversationService.getOrCreateConversation(UserContext.getUserId(), targetUserId);
        return Result.success(conversationId);
    }

    @Operation(summary = "获取会话列表", description = "获取当前行者的所有会话列表")
    @GetMapping
    public Result<List<ConversationVO>> listConversations() {
        Long userId = UserContext.getUserId();
        List<WfConversation> conversations = conversationService.listConversations(userId);

        if (conversations.isEmpty()) {
            return Result.success(new ArrayList<>());
        }

        // 获取所有对方用户的ID
        List<Long> targetUserIds = conversations.stream()
                .map(conv -> conversationService.getTargetUserId(conv, userId))
                .collect(Collectors.toList());

        // 批量查询用户信息
        Map<Long, WfUser> userMap = conversationService.getUserMap(targetUserIds);

        // 构建 VO
        List<ConversationVO> result = new ArrayList<>();
        for (WfConversation conversation : conversations) {
            Long targetUserId = conversationService.getTargetUserId(conversation, userId);
            WfUser targetUser = userMap.get(targetUserId);

            if (targetUser != null) {
                ConversationVO vo = new ConversationVO();
                vo.setConversationId(conversation.getId());
                vo.setTargetUserId(targetUserId);
                vo.setTargetWolfNo(targetUser.getWolfNo());
                vo.setTargetNickname(targetUser.getNickname());
                vo.setTargetAvatar(targetUser.getAvatar());
                vo.setLastMessage(conversation.getLastMessage());
                vo.setLastMessageTime(conversation.getLastMessageTime());
                result.add(vo);
            }
        }

        return Result.success(result);
    }
}

