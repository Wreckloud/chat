package com.wreckloud.wolfchat.chat.websocket.handler.type;

import com.wreckloud.wolfchat.chat.websocket.dto.WsRequest;
import com.wreckloud.wolfchat.chat.websocket.enums.WsType;
import com.wreckloud.wolfchat.chat.websocket.handler.WsTypeRequestHandler;
import com.wreckloud.wolfchat.chat.websocket.handler.support.WsOfflineReplaySupport;
import com.wreckloud.wolfchat.chat.websocket.handler.support.WsResponseSupport;
import com.wreckloud.wolfchat.chat.websocket.handler.support.WsSessionAuthSupport;
import com.wreckloud.wolfchat.chat.websocket.session.WsSessionManager;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.web.socket.WebSocketSession;

/**
 * @Description AUTH 类型 WS 请求处理
 * @Author Wreckloud
 * @Date 2026-03-23
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class WsAuthRequestHandler implements WsTypeRequestHandler {
    private final WsSessionAuthSupport wsSessionAuthSupport;
    private final WsSessionManager sessionManager;
    private final WsResponseSupport wsResponseSupport;
    private final WsOfflineReplaySupport wsOfflineReplaySupport;

    @Override
    public WsType type() {
        return WsType.AUTH;
    }

    @Override
    public void handle(WebSocketSession session, WsRequest request) {
        Long userId = wsSessionAuthSupport.authenticate(session, request.getToken());
        if (userId == null) {
            return;
        }

        sessionManager.addSession(userId, session);
        log.debug("WS 认证成功: userId={}, sessionId={}", userId, session.getId());
        wsResponseSupport.sendAuthOk(session);
        wsOfflineReplaySupport.replayUndeliveredMessages(session, userId);
    }
}

