package com.wreckloud.wolfchat.chat.message.api.controller;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.wreckloud.wolfchat.account.application.service.UserService;
import com.wreckloud.wolfchat.account.domain.entity.WfUser;
import com.wreckloud.wolfchat.chat.message.api.converter.MessageConverter;
import com.wreckloud.wolfchat.chat.message.api.dto.SendMessageDTO;
import com.wreckloud.wolfchat.chat.message.api.vo.MessageVO;
import com.wreckloud.wolfchat.chat.message.application.command.SendMessageCommand;
import com.wreckloud.wolfchat.chat.message.application.service.MessageMediaService;
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
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

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
    private final MessageMediaService messageMediaService;
    private final UserService userService;

    @Operation(summary = "发送消息", description = "在指定会话中发送消息")
    @PostMapping
    public Result<MessageVO> sendMessage(
            @PathVariable Long conversationId,
            @Valid @RequestBody SendMessageDTO dto) {
        Long userId = UserContext.getRequiredUserId();
        WfMessage message = messageService.sendMessage(buildSendCommand(userId, conversationId, dto));
        WfUser sender = userService.getByIdOrThrow(userId);
        MessageVO messageVO = MessageConverter.toMessageVO(message, sender);
        return Result.success(messageMediaService.fillMedia(messageVO));
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
        List<Long> senderIds = messagePage.getRecords().stream()
                .map(WfMessage::getSenderId)
                .distinct()
                .collect(Collectors.toList());
        Map<Long, WfUser> senderMap = userService.getUserMap(senderIds);
        Page<MessageVO> result = MessageConverter.toMessageVOPage(messagePage, senderMap);
        messageMediaService.fillMedia(result.getRecords());
        return Result.success(result);
    }

    private SendMessageCommand buildSendCommand(Long userId, Long conversationId, SendMessageDTO dto) {
        SendMessageCommand command = new SendMessageCommand();
        command.setUserId(userId);
        command.setConversationId(conversationId);
        command.setContent(dto.getContent());
        command.setMsgType(dto.getMsgType());
        command.setMediaKey(dto.getMediaKey());
        command.setMediaWidth(dto.getMediaWidth());
        command.setMediaHeight(dto.getMediaHeight());
        command.setMediaSize(dto.getMediaSize());
        command.setMediaMimeType(dto.getMediaMimeType());
        return command;
    }
}

