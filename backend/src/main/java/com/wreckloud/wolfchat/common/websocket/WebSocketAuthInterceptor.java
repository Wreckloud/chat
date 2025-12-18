package com.wreckloud.wolfchat.common.websocket;

import com.wreckloud.wolfchat.common.util.JwtUtil;
import lombok.RequiredArgsConstructor;
import org.springframework.http.server.ServletServerHttpRequest;
import org.springframework.util.StringUtils;
import org.springframework.web.socket.WebSocketHandler;
import org.springframework.web.socket.server.HandshakeInterceptor;

import javax.servlet.http.HttpServletRequest;
import java.util.Map;
import java.util.UUID;

/**
 * @Description WebSocket 握手拦截器，负责JWT鉴权和deviceId提取
 * @Author Wreckloud
 * @Date 2025-12-08
 */
@RequiredArgsConstructor
public class WebSocketAuthInterceptor implements HandshakeInterceptor {

    private final JwtUtil jwtUtil;

    @Override
    public boolean beforeHandshake(org.springframework.http.server.ServerHttpRequest request,
                                   org.springframework.http.server.ServerHttpResponse response,
                                   WebSocketHandler wsHandler,
                                   Map<String, Object> attributes) {
        if (!(request instanceof ServletServerHttpRequest servletRequest)) {
            return false;
        }
        HttpServletRequest httpRequest = servletRequest.getServletRequest();

        String token = resolveToken(httpRequest);
        String deviceId = resolveDeviceId(httpRequest);

        // token 可选；存在则校验并注入 userId
        if (StringUtils.hasText(token)) {
            if (!jwtUtil.validateToken(token)) {
                return false;
            }
            Long userId = jwtUtil.getUserIdFromToken(token);
            if (userId == null) {
                return false;
            }
            attributes.put("userId", userId);
            attributes.put("token", token);
        }

        attributes.put("deviceId", StringUtils.hasText(deviceId) ? deviceId : "unknown");
        attributes.put("sessionId", UUID.randomUUID().toString());
        return true;
    }

    @Override
    public void afterHandshake(org.springframework.http.server.ServerHttpRequest request,
                               org.springframework.http.server.ServerHttpResponse response,
                               WebSocketHandler wsHandler,
                               Exception exception) {
        // no-op
    }

    private String resolveToken(HttpServletRequest request) {
        String bearer = request.getHeader("Authorization");
        if (StringUtils.hasText(bearer) && bearer.startsWith("Bearer ")) {
            return bearer.substring(7);
        }
        String token = request.getParameter("token");
        if (StringUtils.hasText(token)) {
            return token;
        }
        return null;
    }

    private String resolveDeviceId(HttpServletRequest request) {
        String deviceId = request.getParameter("deviceId");
        if (StringUtils.hasText(deviceId)) {
            return deviceId;
        }
        return request.getHeader("X-Device-Id");
    }
}

