package com.wreckloud.wolfchat.common.security.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.wreckloud.wolfchat.account.domain.entity.WfUser;
import com.wreckloud.wolfchat.account.domain.enums.UserStatus;
import com.wreckloud.wolfchat.account.infra.mapper.WfUserMapper;
import com.wreckloud.wolfchat.common.excption.BaseException;
import com.wreckloud.wolfchat.common.excption.ErrorCode;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * @Description 会话用户有效性服务
 * @Author Wreckloud
 * @Date 2026-03-05
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class SessionUserService {
    private static final long USER_EXIST_CACHE_TTL_MILLIS = 60_000L;
    private static final int USER_EXIST_CACHE_MAX_SIZE = 5000;
    private static final long DB_CHECK_ERROR_LOG_INTERVAL_MILLIS = 10_000L;

    private final WfUserMapper wfUserMapper;
    private final Map<Long, Long> userExistCache = new ConcurrentHashMap<>();
    private volatile long nextDbErrorLogAt = 0L;

    public boolean isSessionUserExists(Long userId) {
        if (userId == null) {
            return false;
        }

        long now = System.currentTimeMillis();
        Long expireAt = userExistCache.get(userId);
        if (expireAt != null && expireAt > now) {
            return true;
        }

        boolean exists = queryUserExists(userId);
        if (exists) {
            if (userExistCache.size() >= USER_EXIST_CACHE_MAX_SIZE) {
                userExistCache.clear();
            }
            userExistCache.put(userId, now + USER_EXIST_CACHE_TTL_MILLIS);
        } else {
            userExistCache.remove(userId);
        }
        return exists;
    }

    public void invalidateUserCache(Long userId) {
        if (userId == null) {
            return;
        }
        userExistCache.remove(userId);
    }

    private boolean queryUserExists(Long userId) {
        LambdaQueryWrapper<WfUser> queryWrapper = new LambdaQueryWrapper<>();
        queryWrapper.eq(WfUser::getId, userId)
                .eq(WfUser::getStatus, UserStatus.NORMAL);
        try {
            return wfUserMapper.selectCount(queryWrapper) > 0;
        } catch (Exception e) {
            long now = System.currentTimeMillis();
            if (now >= nextDbErrorLogAt) {
                nextDbErrorLogAt = now + DB_CHECK_ERROR_LOG_INTERVAL_MILLIS;
                log.warn("会话用户校验失败: userId={}", userId, e);
            }
            throw new BaseException(ErrorCode.SYSTEM_ERROR);
        }
    }
}
