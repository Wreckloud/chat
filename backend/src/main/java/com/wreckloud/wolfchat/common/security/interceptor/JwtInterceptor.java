package com.wreckloud.wolfchat.common.security.interceptor;

import com.wreckloud.wolfchat.common.excption.BaseException;
import com.wreckloud.wolfchat.common.excption.ErrorCode;
import com.wreckloud.wolfchat.common.security.context.UserContext;
import com.wreckloud.wolfchat.common.security.service.SessionUserService;
import com.wreckloud.wolfchat.common.util.JwtUtil;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;
import org.springframework.web.servlet.HandlerInterceptor;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

/**
 * @Description JWT 拦截器，用于校验 token 并设置用户上下文
 * @Author Wreckloud
 * @Date 2026-01-06
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class JwtInterceptor implements HandlerInterceptor {
    private final JwtUtil jwtUtil;
    private final SessionUserService sessionUserService;

    @Override
    public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler) {
        // 获取 token
        String token = getTokenFromRequest(request);
        if (!StringUtils.hasText(token)) {
            throw new BaseException(ErrorCode.UNAUTHORIZED);
        }

        // 验证 token
        if (!jwtUtil.validateToken(token)) {
            throw new BaseException(ErrorCode.TOKEN_INVALID);
        }

        // 设置用户上下文
        Long userId = jwtUtil.getUserIdFromToken(token);
        if (userId == null) {
            throw new BaseException(ErrorCode.TOKEN_INVALID);
        }
        Long tokenPasswordVersion = jwtUtil.getPasswordVersionFromToken(token);
        if (tokenPasswordVersion == null) {
            throw new BaseException(ErrorCode.TOKEN_INVALID);
        }
        if (!sessionUserService.isSessionUserExists(userId)) {
            throw new BaseException(ErrorCode.TOKEN_INVALID);
        }
        if (!sessionUserService.isPasswordVersionMatched(userId, tokenPasswordVersion)) {
            throw new BaseException(ErrorCode.TOKEN_INVALID);
        }
        UserContext.setUserId(userId);

        return true;
    }

    @Override
    public void afterCompletion(HttpServletRequest request, HttpServletResponse response, Object handler, Exception ex) {
        // 清除用户上下文
        UserContext.clear();
    }

    /**
     * 从请求中获取 token
     */
    private String getTokenFromRequest(HttpServletRequest request) {
        String bearerToken = request.getHeader("Authorization");
        if (!StringUtils.hasText(bearerToken)) {
            return null;
        }
        String normalizedToken = bearerToken.trim();
        if (normalizedToken.startsWith("Bearer ")) {
            return normalizedToken.substring(7).trim();
        }
        return null;
    }
}


