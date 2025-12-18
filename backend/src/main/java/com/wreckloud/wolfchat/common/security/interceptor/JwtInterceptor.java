package com.wreckloud.wolfchat.common.security.interceptor;

import com.wreckloud.wolfchat.common.excption.BaseException;
import com.wreckloud.wolfchat.common.excption.ErrorCode;
import com.wreckloud.wolfchat.common.security.annotation.RequireLogin;
import com.wreckloud.wolfchat.common.security.context.UserContext;
import com.wreckloud.wolfchat.common.util.JwtUtil;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;
import org.springframework.web.method.HandlerMethod;
import org.springframework.web.servlet.HandlerInterceptor;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

/**
 * @Description JWT拦截器，用于验证Token并解析用户信息
 *
 * @author Wreckloud
 * @date 2024-12-18
 */
@Slf4j
@Component
public class JwtInterceptor implements HandlerInterceptor {

    @Autowired
    private JwtUtil jwtUtil;

    private static final String AUTHORIZATION_HEADER = "Authorization";
    private static final String BEARER_PREFIX = "Bearer ";

    @Override
    public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler) throws Exception {
        // 如果不是方法处理器，直接放行
        if (!(handler instanceof HandlerMethod)) {
            return true;
        }

        HandlerMethod handlerMethod = (HandlerMethod) handler;

        // 检查方法或类上是否有@RequireLogin注解
        RequireLogin methodAnnotation = handlerMethod.getMethodAnnotation(RequireLogin.class);
        RequireLogin classAnnotation = handlerMethod.getBeanType().getAnnotation(RequireLogin.class);

        // 如果没有@RequireLogin注解，直接放行
        if (methodAnnotation == null && classAnnotation == null) {
            return true;
        }

        // 检查是否required=false
        if ((methodAnnotation != null && !methodAnnotation.required()) ||
            (classAnnotation != null && !classAnnotation.required())) {
            return true;
        }

        // 1. 从Header获取Token
        String authHeader = request.getHeader(AUTHORIZATION_HEADER);
        if (!StringUtils.hasText(authHeader)) {
            log.warn("请求未携带Authorization header，路径: {}", request.getRequestURI());
            throw new BaseException(ErrorCode.UNAUTHORIZED);
        }

        // 2. 检查Token格式（Bearer xxx）
        if (!authHeader.startsWith(BEARER_PREFIX)) {
            log.warn("Authorization header格式错误，路径: {}", request.getRequestURI());
            throw new BaseException(ErrorCode.TOKEN_INVALID);
        }

        // 3. 提取Token
        String token = authHeader.substring(BEARER_PREFIX.length());
        if (!StringUtils.hasText(token)) {
            log.warn("Token为空，路径: {}", request.getRequestURI());
            throw new BaseException(ErrorCode.TOKEN_INVALID);
        }

        // 4. 验证Token有效性
        if (!jwtUtil.validateToken(token)) {
            log.warn("Token无效或已过期，路径: {}", request.getRequestURI());
            throw new BaseException(ErrorCode.TOKEN_EXPIRED);
        }

        // 5. 解析userId
        Long userId = jwtUtil.getUserIdFromToken(token);
        if (userId == null) {
            log.warn("无法从Token中解析userId，路径: {}", request.getRequestURI());
            throw new BaseException(ErrorCode.TOKEN_INVALID);
        }

        // 6. 存入ThreadLocal
        UserContext.setUserId(userId);
        log.debug("用户认证成功，userId: {}, 路径: {}", userId, request.getRequestURI());

        return true;
    }

    @Override
    public void afterCompletion(HttpServletRequest request, HttpServletResponse response, Object handler, Exception ex) throws Exception {
        // 请求完成后清除ThreadLocal，避免内存泄漏
        UserContext.clear();
    }
}

