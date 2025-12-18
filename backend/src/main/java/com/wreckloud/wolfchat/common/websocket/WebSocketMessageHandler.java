package com.wreckloud.wolfchat.common.websocket;

import lombok.extern.slf4j.Slf4j;
import org.springframework.web.socket.CloseStatus;
import org.springframework.web.socket.PingMessage;
import org.springframework.web.socket.PongMessage;
import org.springframework.web.socket.TextMessage;
import org.springframework.web.socket.WebSocketSession;
import org.springframework.web.socket.handler.AbstractWebSocketHandler;

import java.nio.ByteBuffer;
import java.time.Instant;

/**
 * @Description WebSocket 消息处理，支持心跳与同设备踢旧
 * @Author Wreckloud
 * @Date 2025-12-08
 */
@Slf4j
public class WebSocketMessageHandler extends AbstractWebSocketHandler {

    private static final String ATTR_USER_ID = "userId";
    private static final String ATTR_DEVICE_ID = "deviceId";
    private static final String ATTR_SESSION_ID = "sessionId";

    @Override
    public void afterConnectionEstablished(WebSocketSession session) throws Exception {
        Long userId = (Long) session.getAttributes().get(ATTR_USER_ID);
        String deviceId = (String) session.getAttributes().get(ATTR_DEVICE_ID);
        String sessionId = (String) session.getAttributes().get(ATTR_SESSION_ID);

        log.info("[WS] connected userId={}, deviceId={}, session={}", userId, deviceId, sessionId);
    }

    @Override
    protected void handleTextMessage(WebSocketSession session, TextMessage message) throws Exception {
        String payload = message.getPayload();
        Long userId = (Long) session.getAttributes().get(ATTR_USER_ID);
        String deviceId = (String) session.getAttributes().get(ATTR_DEVICE_ID);

        // 应用层心跳 fallback
        if ("ping".equalsIgnoreCase(payload)) {
            session.sendMessage(new TextMessage("pong"));
            return;
        }

        // 其他业务消息暂不处理，回一个简单确认
        session.sendMessage(new TextMessage("ack:" + Instant.now().toEpochMilli()));
    }

    @Override
    protected void handlePongMessage(WebSocketSession session, PongMessage message) throws Exception {
        // 更新心跳时间的需求已简化，无需持久化
    }

    @Override
    protected void handleBinaryMessage(WebSocketSession session, org.springframework.web.socket.BinaryMessage message) throws Exception {
        // 如果客户端发送了 ping（二进制 ping），回复 pong
        ByteBuffer buf = message.getPayload();
        if (buf.remaining() == 4 && buf.get(0) == 'p' && buf.get(1) == 'i') {
            session.sendMessage(new PongMessage());
        }
    }

    @Override
    public void handleTransportError(WebSocketSession session, Throwable exception) throws Exception {
        log.warn("[WS] transport error sessionId={}, ex={}", session.getId(), exception.getMessage());
    }

    @Override
    public void afterConnectionClosed(WebSocketSession session, CloseStatus status) throws Exception {
        Long userId = (Long) session.getAttributes().get(ATTR_USER_ID);
        String deviceId = (String) session.getAttributes().get(ATTR_DEVICE_ID);
        log.info("[WS] closed userId={}, deviceId={}, status={}", userId, deviceId, status);
    }

    @Override
    public boolean supportsPartialMessages() {
        return false;
    }
}

