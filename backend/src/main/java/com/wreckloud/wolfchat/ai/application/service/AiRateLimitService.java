package com.wreckloud.wolfchat.ai.application.service;

import lombok.RequiredArgsConstructor;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import java.time.Duration;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

/**
 * AI 回复节流控制。
 */
@Service
@RequiredArgsConstructor
public class AiRateLimitService {
    private static final String REDIS_PREFIX = "ai:rate:";
    private static final DateTimeFormatter HOUR_FORMAT = DateTimeFormatter.ofPattern("yyyyMMddHH");
    private static final DateTimeFormatter DAY_FORMAT = DateTimeFormatter.ofPattern("yyyyMMdd");

    private final StringRedisTemplate stringRedisTemplate;

    public boolean allowByCooldown(String scene, Long botUserId, String targetKey, Integer cooldownSeconds) {
        if (!isPositive(botUserId) || !StringUtils.hasText(scene) || !StringUtils.hasText(targetKey)) {
            return false;
        }
        int safeCooldownSeconds = cooldownSeconds == null || cooldownSeconds <= 0 ? 30 : cooldownSeconds;
        String key = REDIS_PREFIX + "cd:" + scene + ":" + botUserId + ":" + targetKey.trim();
        Boolean created = stringRedisTemplate.opsForValue()
                .setIfAbsent(key, "1", Duration.ofSeconds(safeCooldownSeconds));
        return Boolean.TRUE.equals(created);
    }

    public boolean allowByHourlyLimit(String scene, Long botUserId, Integer maxRepliesPerHour) {
        if (!isPositive(botUserId) || !StringUtils.hasText(scene)) {
            return false;
        }
        int safeMax = maxRepliesPerHour == null || maxRepliesPerHour <= 0 ? 30 : maxRepliesPerHour;
        String hourBucket = LocalDateTime.now().format(HOUR_FORMAT);
        String key = REDIS_PREFIX + "hour:" + scene + ":" + botUserId + ":" + hourBucket;
        return allowCounterLimit(key, safeMax, Duration.ofHours(2));
    }

    public boolean allowByGlobalHourlyLimit(Long botUserId, Integer maxCallsPerHour) {
        if (!isPositive(botUserId)) {
            return false;
        }
        int safeMax = maxCallsPerHour == null || maxCallsPerHour <= 0 ? 80 : maxCallsPerHour;
        String hourBucket = LocalDateTime.now().format(HOUR_FORMAT);
        String key = REDIS_PREFIX + "global:hour:" + botUserId + ":" + hourBucket;
        return allowCounterLimit(key, safeMax, Duration.ofHours(2));
    }

    public boolean allowByGlobalDailyLimit(Long botUserId, Integer maxCallsPerDay) {
        if (!isPositive(botUserId)) {
            return false;
        }
        int safeMax = maxCallsPerDay == null || maxCallsPerDay <= 0 ? 600 : maxCallsPerDay;
        String dayBucket = LocalDateTime.now().format(DAY_FORMAT);
        String key = REDIS_PREFIX + "global:day:" + botUserId + ":" + dayBucket;
        return allowCounterLimit(key, safeMax, Duration.ofDays(2));
    }

    private boolean allowCounterLimit(String key, int maxAllowed, Duration ttl) {
        Long count = stringRedisTemplate.opsForValue().increment(key);
        if (count == null) {
            return false;
        }
        if (count == 1L) {
            stringRedisTemplate.expire(key, ttl);
        }
        return count <= maxAllowed;
    }

    private boolean isPositive(Long value) {
        return value != null && value > 0L;
    }
}
