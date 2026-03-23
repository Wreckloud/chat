package com.wreckloud.wolfchat.chat.websocket.handler;

import com.alibaba.fastjson.JSON;
import com.wreckloud.wolfchat.chat.websocket.dto.WsRequest;
import com.wreckloud.wolfchat.chat.websocket.enums.WsType;
import com.wreckloud.wolfchat.chat.websocket.handler.support.WsResponseSupport;
import com.wreckloud.wolfchat.chat.websocket.session.WsSessionManager;
import com.wreckloud.wolfchat.common.excption.ErrorCode;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.lang.NonNull;
import org.springframework.stereotype.Component;
import org.springframework.web.socket.CloseStatus;
import org.springframework.web.socket.TextMessage;
import org.springframework.web.socket.WebSocketSession;
import org.springframework.web.socket.handler.TextWebSocketHandler;

import javax.annotation.PostConstruct;
import java.util.EnumMap;
import java.util.List;
import java.util.Map;

/**
 * @Description WebSocket 消息处理
 * @Author Wreckloud
 * @Date 2026-02-05
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class ChatWebSocketHandler extends TextWebSocketHandler {
    private final List<WsTypeRequestHandler> wsTypeRequestHandlers;
    private final WsResponseSupport wsResponseSupport;
    private final WsSessionManager sessionManager;
    private final Map<WsType, WsTypeRequestHandler> requestHandlerMap = new EnumMap<>(WsType.class);

    @PostConstruct
    public void initRequestHandlers() {
        for (WsTypeRequestHandler handler : wsTypeRequestHandlers) {
            WsType wsType = handler.type();
            if (requestHandlerMap.putIfAbsent(wsType, handler) != null) {
                throw new IllegalStateException("重复的 WS 处理器注册: " + wsType);
            }
        }
    }

    @Override
    public void afterConnectionEstablished(@NonNull WebSocketSession session) {
        log.debug("WS 连接建立: sessionId={}", session.getId());
    }

    @Override
    protected void handleTextMessage(@NonNull WebSocketSession session, @NonNull TextMessage message) {
        WsRequest request = parseRequest(session, message.getPayload());
        if (request == null) {
            return;
        }

        WsTypeRequestHandler requestHandler = requestHandlerMap.get(request.getType());
        if (requestHandler == null) {
            wsResponseSupport.sendError(session, ErrorCode.PARAM_ERROR, "不支持的消息类型");
            return;
        }
        try {
            requestHandler.handle(session, request);
        } catch (Exception e) {
            log.error("WS 请求处理异常: type={}, sessionId={}",
                    request.getType(), session.getId(), e);
            wsResponseSupport.sendError(session, ErrorCode.SYSTEM_ERROR, "系统错误", request.getClientMsgId());
        }
    }

    @Override
    public void afterConnectionClosed(@NonNull WebSocketSession session, @NonNull CloseStatus status) {
        sessionManager.removeSession(session);
        log.debug("WS 连接关闭: sessionId={}, status={}", session.getId(), status);
    }

    private WsRequest parseRequest(WebSocketSession session, String payload) {
        WsRequest request;
        try {
            request = JSON.parseObject(payload, WsRequest.class);
        } catch (Exception e) {
            wsResponseSupport.sendError(session, ErrorCode.PARAM_ERROR, "消息格式错误");
            return null;
        }

        if (request == null || request.getType() == null) {
            wsResponseSupport.sendError(session, ErrorCode.PARAM_ERROR, "消息类型不能为空");
            return null;
        }
        return request;
    }
}
