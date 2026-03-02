package com.wreckloud.wolfchat.chat.websocket.session;

import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.web.socket.TextMessage;
import org.springframework.web.socket.WebSocketSession;

import java.io.IOException;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

/**
 * @Description WebSocket 会话管理
 * @Author Wreckloud
 * @Date 2026-02-05
 */
@Slf4j
@Component
public class WsSessionManager {
    private final Map<Long, Set<WebSocketSession>> userSessions = new ConcurrentHashMap<>();
    private final Map<String, Long> sessionUserMap = new ConcurrentHashMap<>();

    public void addSession(Long userId, WebSocketSession session) {
        String sessionId = session.getId();
        Long existingUserId = sessionUserMap.get(sessionId);
        if (existingUserId != null && !existingUserId.equals(userId)) {
            removeSession(session);
            log.info("WS 重绑会话: oldUserId={}, newUserId={}, sessionId={}", existingUserId, userId, sessionId);
        }
        userSessions.computeIfAbsent(userId, key -> ConcurrentHashMap.newKeySet()).add(session);
        sessionUserMap.put(sessionId, userId);
        log.info("WS 绑定会话: userId={}, sessionId={}", userId, sessionId);
    }

    public void removeSession(WebSocketSession session) {
        Long userId = sessionUserMap.remove(session.getId());
        if (userId == null) {
            return;
        }
        Set<WebSocketSession> sessions = userSessions.get(userId);
        if (sessions != null) {
            sessions.remove(session);
            if (sessions.isEmpty()) {
                userSessions.remove(userId);
            }
        }
        log.info("WS 解绑会话: userId={}, sessionId={}", userId, session.getId());
    }

    public Long getUserId(WebSocketSession session) {
        return sessionUserMap.get(session.getId());
    }

    public int sendToUser(Long userId, String payload) {
        Set<WebSocketSession> sessions = userSessions.get(userId);
        if (sessions == null || sessions.isEmpty()) {
            return 0;
        }
        TextMessage message = new TextMessage(payload);
        int successCount = 0;
        for (WebSocketSession session : sessions) {
            if (!session.isOpen()) {
                removeSession(session);
                continue;
            }
            try {
                session.sendMessage(message);
                successCount++;
            } catch (IOException e) {
                log.warn("WS 推送失败: userId={}, sessionId={}, error={}", userId, session.getId(), e.getMessage());
                removeSession(session);
            }
        }
        return successCount;
    }
}
