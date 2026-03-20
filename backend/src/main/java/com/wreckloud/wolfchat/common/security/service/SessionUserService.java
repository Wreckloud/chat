package com.wreckloud.wolfchat.common.security.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.wreckloud.wolfchat.account.domain.entity.WfUserAuth;
import com.wreckloud.wolfchat.account.domain.entity.WfUser;
import com.wreckloud.wolfchat.account.domain.enums.UserAuthType;
import com.wreckloud.wolfchat.account.domain.enums.UserStatus;
import com.wreckloud.wolfchat.account.infra.mapper.WfUserAuthMapper;
import com.wreckloud.wolfchat.account.infra.mapper.WfUserMapper;
import com.wreckloud.wolfchat.common.excption.BaseException;
import com.wreckloud.wolfchat.common.excption.ErrorCode;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.time.ZoneId;
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
    private final WfUserAuthMapper wfUserAuthMapper;
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

    public boolean isPasswordVersionMatched(Long userId, Long tokenPasswordVersion) {
        if (userId == null || tokenPasswordVersion == null) {
            return false;
        }
        long latestPasswordVersion = queryLatestPasswordVersion(userId);
        return tokenPasswordVersion >= latestPasswordVersion;
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

    private long queryLatestPasswordVersion(Long userId) {
        LambdaQueryWrapper<WfUserAuth> queryWrapper = new LambdaQueryWrapper<>();
        queryWrapper.select(WfUserAuth::getUpdateTime)
                .eq(WfUserAuth::getUserId, userId)
                .eq(WfUserAuth::getEnabled, true)
                .in(WfUserAuth::getAuthType, UserAuthType.WOLF_NO_PASSWORD, UserAuthType.EMAIL_PASSWORD)
                .orderByDesc(WfUserAuth::getUpdateTime)
                .orderByDesc(WfUserAuth::getId)
                .last("LIMIT 1");
        try {
            WfUserAuth latestAuth = wfUserAuthMapper.selectOne(queryWrapper);
            if (latestAuth == null || latestAuth.getUpdateTime() == null) {
                return 0L;
            }
            return latestAuth.getUpdateTime().atZone(ZoneId.systemDefault()).toEpochSecond();
        } catch (Exception e) {
            long now = System.currentTimeMillis();
            if (now >= nextDbErrorLogAt) {
                nextDbErrorLogAt = now + DB_CHECK_ERROR_LOG_INTERVAL_MILLIS;
                log.warn("会话密码版本校验失败: userId={}", userId, e);
            }
            throw new BaseException(ErrorCode.SYSTEM_ERROR);
        }
    }
}
