package com.wreckloud.wolfchat.chat.websocket.handler.support;

import com.wreckloud.wolfchat.account.application.service.UserService;
import com.wreckloud.wolfchat.account.domain.entity.WfUser;
import com.wreckloud.wolfchat.chat.message.api.converter.MessageConverter;
import com.wreckloud.wolfchat.chat.message.api.vo.MessageVO;
import com.wreckloud.wolfchat.chat.message.application.service.MessageMediaService;
import com.wreckloud.wolfchat.chat.message.domain.entity.WfMessage;
import com.wreckloud.wolfchat.chat.websocket.dto.UploadProgressPayload;
import com.wreckloud.wolfchat.chat.websocket.dto.WsRequest;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;

/**
 * @Description WebSocket 消息视图构建支持
 * @Author Wreckloud
 * @Date 2026-03-23
 */
@Component
@RequiredArgsConstructor
public class WsMessageBuildSupport {
    private final UserService userService;
    private final MessageMediaService messageMediaService;

    public MessageVO buildMessageVOWithSender(WfMessage message) {
        WfUser sender = null;
        Long senderId = message.getSenderId();
        if (senderId != null && senderId > 0L) {
            sender = userService.getByIdOrThrow(senderId);
        }
        return messageMediaService.fillMedia(MessageConverter.toMessageVO(message, sender));
    }

    public UploadProgressPayload buildUploadProgressPayload(Long userId, WsRequest request, String sendType) {
        WfUser sender = userService.getByIdOrThrow(userId);
        UploadProgressPayload payload = new UploadProgressPayload();
        payload.setSendType(sendType);
        payload.setConversationId(request.getConversationId());
        payload.setClientMsgId(request.getClientMsgId());
        payload.setSenderId(userId);
        payload.setSenderWolfNo(sender.getWolfNo());
        payload.setSenderNickname(sender.getNickname());
        payload.setSenderEquippedTitleName(sender.getEquippedTitleName());
        payload.setSenderEquippedTitleColor(sender.getEquippedTitleColor());
        payload.setSenderAvatar(sender.getAvatar());
        payload.setMsgType(request.getMsgType());
        payload.setUploadProgress(request.getUploadProgress());
        payload.setUploadStatus(request.getUploadStatus() == null ? null : request.getUploadStatus().trim().toUpperCase());
        payload.setCreateTime(LocalDateTime.now());
        return payload;
    }
}

