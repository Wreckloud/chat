package com.wreckloud.wolfchat.common.security.interceptor;

import com.wreckloud.wolfchat.account.domain.entity.WfUser;
import com.wreckloud.wolfchat.account.infra.mapper.WfUserMapper;
import com.wreckloud.wolfchat.common.config.AdminProperties;
import com.wreckloud.wolfchat.common.excption.BaseException;
import com.wreckloud.wolfchat.common.excption.ErrorCode;
import com.wreckloud.wolfchat.common.security.context.UserContext;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import org.springframework.web.servlet.HandlerInterceptor;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

/**
 * @Description 管理员权限拦截器 - 基于RBAC
 * @Author Wreckloud
 * @Date 2024-12-18
 * 
 * 权限判断逻辑：
 * 1. 优先检查配置文件中的超级管理员WF号
 * 2. 其次检查数据库中的role字段（role >= 2）
 * 3. 都不满足则拒绝访问
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class AdminInterceptor implements HandlerInterceptor {
    
    private final AdminProperties adminProperties;
    private final WfUserMapper userMapper;
    
    @Override
    public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler) {
        // 1. 获取当前登录用户ID
        Long userId = UserContext.getUserId();
        if (userId == null) {
            log.warn("管理接口访问失败：用户未登录");
            throw new BaseException(ErrorCode.UNAUTHORIZED);
        }
        
        // 2. 查询用户信息
        WfUser user = userMapper.selectById(userId);
        if (user == null) {
            log.error("管理接口访问失败：用户不存在, userId={}", userId);
            throw new BaseException(ErrorCode.USER_NOT_FOUND);
        }
        
        // 3. 检查是否是超级管理员（配置文件）
        if (adminProperties.isSuperAdmin(user.getWfNo())) {
            log.info("超级管理员访问管理接口：userId={}, wfNo={}, path={}", 
                    userId, user.getWfNo(), request.getRequestURI());
            return true;
        }
        
        // 4. 检查role字段（数据库）
        if (user.getRole() != null && user.getRole() >= 2) {
            log.info("管理员访问管理接口：userId={}, wfNo={}, role={}, path={}", 
                    userId, user.getWfNo(), user.getRole(), request.getRequestURI());
            return true;
        }
        
        // 5. 拒绝访问
        log.warn("非管理员尝试访问管理接口：userId={}, wfNo={}, role={}, path={}", 
                userId, user.getWfNo(), user.getRole(), request.getRequestURI());
        throw new BaseException(ErrorCode.NO_PERMISSION);
    }
}

