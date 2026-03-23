package com.wreckloud.wolfchat.chat.websocket.handler.support;

import com.wreckloud.wolfchat.chat.websocket.session.WsSessionManager;
import com.wreckloud.wolfchat.common.excption.ErrorCode;
import com.wreckloud.wolfchat.common.security.service.SessionUserService;
import com.wreckloud.wolfchat.common.util.JwtUtil;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;
import org.springframework.web.socket.WebSocketSession;

/**
 * @Description WebSocket 会话认证与登录态校验支持
 * @Author Wreckloud
 * @Date 2026-03-23
 */
@Component
@RequiredArgsConstructor
public class WsSessionAuthSupport {
    public static final String SESSION_PASSWORD_VERSION_KEY = "session_pwd_ver";

    private final JwtUtil jwtUtil;
    private final SessionUserService sessionUserService;
    private final WsSessionManager sessionManager;
    private final WsResponseSupport wsResponseSupport;

    public Long authenticate(WebSocketSession session, String rawToken) {
        String token = normalizeToken(rawToken);
        if (!StringUtils.hasText(token)) {
            wsResponseSupport.sendError(session, ErrorCode.UNAUTHORIZED, "token 不能为空");
            return null;
        }
        if (!jwtUtil.validateToken(token)) {
            wsResponseSupport.sendError(session, ErrorCode.TOKEN_INVALID, "token 无效");
            return null;
        }

        Long userId = jwtUtil.getUserIdFromToken(token);
        Long tokenPasswordVersion = jwtUtil.getPasswordVersionFromToken(token);
        if (userId == null || tokenPasswordVersion == null) {
            wsResponseSupport.sendError(session, ErrorCode.TOKEN_INVALID, "token 无效");
            return null;
        }
        if (!sessionUserService.isSessionUserExists(userId)
                || !sessionUserService.isPasswordVersionMatched(userId, tokenPasswordVersion)) {
            wsResponseSupport.sendError(session, ErrorCode.TOKEN_INVALID, "token 无效");
            return null;
        }
        session.getAttributes().put(SESSION_PASSWORD_VERSION_KEY, tokenPasswordVersion);
        return userId;
    }

    public Long requireValidatedUser(WebSocketSession session, String clientMsgId) {
        Long userId = sessionManager.getUserId(session);
        if (userId == null) {
            wsResponseSupport.sendError(session, ErrorCode.UNAUTHORIZED, "请先认证", clientMsgId);
            return null;
        }
        if (!sessionUserService.isSessionUserExists(userId)) {
            wsResponseSupport.sendError(session, ErrorCode.TOKEN_INVALID, "登录状态已失效，请重新登录", clientMsgId);
            sessionManager.removeSession(session);
            return null;
        }
        Long tokenPasswordVersion = (Long) session.getAttributes().get(SESSION_PASSWORD_VERSION_KEY);
        if (!sessionUserService.isPasswordVersionMatched(userId, tokenPasswordVersion)) {
            wsResponseSupport.sendError(session, ErrorCode.TOKEN_INVALID, "登录状态已失效，请重新登录", clientMsgId);
            sessionManager.removeSession(session);
            return null;
        }
        return userId;
    }

    private String normalizeToken(String rawToken) {
        if (!StringUtils.hasText(rawToken)) {
            return null;
        }
        String token = rawToken.trim();
        if (token.startsWith("Bearer ")) {
            token = token.substring(7).trim();
        }
        return token;
    }
}

