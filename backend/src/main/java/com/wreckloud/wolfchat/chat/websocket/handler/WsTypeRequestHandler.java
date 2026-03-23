package com.wreckloud.wolfchat.chat.websocket.handler;

import com.wreckloud.wolfchat.chat.websocket.dto.WsRequest;
import com.wreckloud.wolfchat.chat.websocket.enums.WsType;
import org.springframework.web.socket.WebSocketSession;

/**
 * @Description 按消息类型处理 WebSocket 请求
 * @Author Wreckloud
 * @Date 2026-03-23
 */
public interface WsTypeRequestHandler {
    /**
     * 当前处理器支持的消息类型
     */
    WsType type();

    /**
     * 执行消息处理
     */
    void handle(WebSocketSession session, WsRequest request);
}

