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
        userSessions.computeIfAbsent(userId, key -> ConcurrentHashMap.newKeySet()).add(session);
        sessionUserMap.put(session.getId(), userId);
        log.info("WS 绑定会话: userId={}, sessionId={}", userId, session.getId());
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

    /**
     * 判断用户是否在线
     */
    public boolean isUserOnline(Long userId) {
        Set<WebSocketSession> sessions = userSessions.get(userId);
        return sessions != null && !sessions.isEmpty();
    }

    public void sendToUser(Long userId, String payload) {
        Set<WebSocketSession> sessions = userSessions.get(userId);
        if (sessions == null || sessions.isEmpty()) {
            return;
        }
        TextMessage message = new TextMessage(payload);
        for (WebSocketSession session : sessions) {
            if (!session.isOpen()) {
                removeSession(session);
                continue;
            }
            try {
                session.sendMessage(message);
            } catch (IOException e) {
                log.warn("WS 推送失败: userId={}, sessionId={}, error={}", userId, session.getId(), e.getMessage());
            }
        }
    }
}
