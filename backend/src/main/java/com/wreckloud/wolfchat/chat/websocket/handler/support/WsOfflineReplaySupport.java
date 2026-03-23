package com.wreckloud.wolfchat.chat.websocket.handler.support;

import com.wreckloud.wolfchat.chat.message.application.service.MessageService;
import com.wreckloud.wolfchat.chat.message.domain.entity.WfMessage;
import com.wreckloud.wolfchat.chat.websocket.dto.WsResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.web.socket.WebSocketSession;

import java.util.ArrayList;
import java.util.List;

/**
 * @Description WebSocket 离线消息补发支持
 * @Author Wreckloud
 * @Date 2026-03-23
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class WsOfflineReplaySupport {
    private final MessageService messageService;
    private final WsMessageBuildSupport wsMessageBuildSupport;
    private final WsResponseSupport wsResponseSupport;

    public void replayUndeliveredMessages(WebSocketSession session, Long userId) {
        List<WfMessage> undelivered = messageService.listUndeliveredMessages(userId);
        if (undelivered.isEmpty()) {
            return;
        }

        log.info("WS 补发未送达消息: userId={}, count={}", userId, undelivered.size());
        List<Long> deliveredIds = new ArrayList<>();
        for (WfMessage message : undelivered) {
            WsResponse push = wsResponseSupport.buildMessageResponse(wsMessageBuildSupport.buildMessageVOWithSender(message));
            if (wsResponseSupport.send(session, push)) {
                deliveredIds.add(message.getId());
            }
        }
        if (!deliveredIds.isEmpty()) {
            messageService.markDelivered(deliveredIds);
        }
    }
}

