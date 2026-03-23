package com.wreckloud.wolfchat.chat.websocket.handler.type;

import com.wreckloud.wolfchat.chat.websocket.dto.WsRequest;
import com.wreckloud.wolfchat.chat.websocket.enums.WsType;
import com.wreckloud.wolfchat.chat.websocket.handler.WsTypeRequestHandler;
import com.wreckloud.wolfchat.chat.websocket.handler.support.WsAuthenticatedExecutor;
import com.wreckloud.wolfchat.chat.websocket.session.WsSessionManager;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import org.springframework.web.socket.WebSocketSession;

/**
 * @Description PING 类型 WS 请求处理
 * @Author Wreckloud
 * @Date 2026-03-23
 */
@Component
@RequiredArgsConstructor
public class WsPingRequestHandler implements WsTypeRequestHandler {
    private final WsAuthenticatedExecutor wsAuthenticatedExecutor;
    private final WsSessionManager sessionManager;

    @Override
    public WsType type() {
        return WsType.PING;
    }

    @Override
    public void handle(WebSocketSession session, WsRequest request) {
        wsAuthenticatedExecutor.execute(session, request, "心跳", (userId, clientMsgId, currentRequest) ->
                sessionManager.refreshOnline(userId));
    }
}

