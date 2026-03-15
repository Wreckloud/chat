package com.wreckloud.wolfchat.chat.presence.application.service;

import com.alibaba.fastjson.JSON;
import com.wreckloud.wolfchat.chat.conversation.application.service.ConversationService;
import com.wreckloud.wolfchat.chat.presence.application.event.UserPresenceChangedEvent;
import com.wreckloud.wolfchat.chat.websocket.dto.PresencePayload;
import com.wreckloud.wolfchat.chat.websocket.dto.WsResponse;
import com.wreckloud.wolfchat.chat.websocket.enums.WsType;
import com.wreckloud.wolfchat.chat.websocket.session.WsSessionManager;
import lombok.RequiredArgsConstructor;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Component;

import java.util.List;

/**
 * @Description 在线状态推送服务
 * @Author Wreckloud
 * @Date 2026-03-07
 */
@Component
@RequiredArgsConstructor
public class UserPresencePushService {
    private final ConversationService conversationService;
    private final WsSessionManager wsSessionManager;

    @EventListener
    public void onUserPresenceChanged(UserPresenceChangedEvent event) {
        List<Long> receiverIds = conversationService.listPeerUserIds(event.getUserId());
        if (receiverIds.isEmpty()) {
            return;
        }

        PresencePayload payload = new PresencePayload();
        payload.setUserId(event.getUserId());
        payload.setOnline(event.isOnline());
        payload.setLastSeenAt(event.getLastSeenAt());

        WsResponse response = new WsResponse();
        response.setType(WsType.PRESENCE);
        response.setData(payload);
        String message = JSON.toJSONString(response);
        for (Long receiverId : receiverIds) {
            wsSessionManager.sendToUser(receiverId, message);
        }
    }
}
