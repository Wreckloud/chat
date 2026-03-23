package com.wreckloud.wolfchat.chat.websocket.handler.support;

import com.wreckloud.wolfchat.chat.websocket.dto.WsRequest;
import com.wreckloud.wolfchat.common.excption.BaseException;
import com.wreckloud.wolfchat.common.excption.ErrorCode;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.web.socket.WebSocketSession;

/**
 * @Description 统一执行需要认证的 WS 业务
 * @Author Wreckloud
 * @Date 2026-03-23
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class WsAuthenticatedExecutor {
    private final WsSessionAuthSupport wsSessionAuthSupport;
    private final WsResponseSupport wsResponseSupport;

    @FunctionalInterface
    public interface AuthenticatedAction {
        void execute(Long userId, String clientMsgId, WsRequest request) throws Exception;
    }

    public void execute(WebSocketSession session, WsRequest request, String scene, AuthenticatedAction action) {
        String clientMsgId = request.getClientMsgId();
        Long userId = wsSessionAuthSupport.requireValidatedUser(session, clientMsgId);
        if (userId == null) {
            return;
        }
        try {
            action.execute(userId, clientMsgId, request);
        } catch (BaseException e) {
            wsResponseSupport.sendError(session, e.getCode(), e.getMessage(), clientMsgId);
        } catch (Exception e) {
            log.error("WS {}失败: userId={}, sessionId={}", scene, userId, session.getId(), e);
            wsResponseSupport.sendError(session, ErrorCode.SYSTEM_ERROR, "系统错误", clientMsgId);
        }
    }
}

