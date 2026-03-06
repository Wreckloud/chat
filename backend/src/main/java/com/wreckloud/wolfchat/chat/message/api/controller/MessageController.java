package com.wreckloud.wolfchat.chat.message.api.controller;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.wreckloud.wolfchat.chat.message.api.converter.MessageConverter;
import com.wreckloud.wolfchat.chat.message.api.dto.SendMessageDTO;
import com.wreckloud.wolfchat.chat.message.api.vo.MessageVO;
import com.wreckloud.wolfchat.chat.message.application.service.MessageService;
import com.wreckloud.wolfchat.chat.message.domain.entity.WfMessage;
import com.wreckloud.wolfchat.common.security.context.UserContext;
import com.wreckloud.wolfchat.common.web.Result;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import javax.validation.Valid;

/**
 * @Description 消息控制器
 * @Author Wreckloud
 * @Date 2026-01-29
 */
@Tag(name = "聊天-消息", description = "消息收发相关接口")
@RestController
@RequestMapping("/conversations/{conversationId}/messages")
@RequiredArgsConstructor
public class MessageController {
    private final MessageService messageService;

    @Operation(summary = "发送消息", description = "在指定会话中发送消息")
    @PostMapping
    public Result<MessageVO> sendMessage(
            @PathVariable Long conversationId,
            @Valid @RequestBody SendMessageDTO dto) {
        Long userId = UserContext.getRequiredUserId();
        WfMessage message = messageService.sendMessage(
                userId,
                conversationId,
                dto.getContent()
        );
        return Result.success(MessageConverter.toMessageVO(message));
    }

    @Operation(summary = "消息列表", description = "分页查询会话消息列表")
    @GetMapping
    public Result<Page<MessageVO>> listMessages(
            @PathVariable Long conversationId,
            @RequestParam(defaultValue = "1") Integer page,
            @RequestParam(defaultValue = "20") Integer size) {
        Long userId = UserContext.getRequiredUserId();
        Page<WfMessage> messagePage = messageService.listMessages(
                userId,
                conversationId,
                page,
                size
        );
        return Result.success(MessageConverter.toMessageVOPage(messagePage));
    }

}

