package com.wreckloud.wolfchat.ai.application.support;

import com.wreckloud.wolfchat.ai.application.service.AiRateLimitService;
import com.wreckloud.wolfchat.ai.config.AiConfig;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

/**
 * @Description AI 触发守卫支持
 * @Author Wreckloud
 * @Date 2026-03-23
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class AiTriggerGuardSupport {
    private final AiConfig aiConfig;
    private final AiRateLimitService aiRateLimitService;

    public boolean hitProbability(Double configuredProbability, double defaultProbability) {
        double probability = configuredProbability == null ? defaultProbability : configuredProbability;
        if (probability <= 0D) {
            return false;
        }
        if (probability >= 1D) {
            return true;
        }
        return Math.random() < probability;
    }

    public boolean allowByCooldown(String scene, Long botUserId, String target, Integer cooldownSeconds) {
        return aiRateLimitService.allowByCooldown(scene, botUserId, target, cooldownSeconds);
    }

    public boolean allowByHourlyLimit(String scene, Long botUserId, Integer maxRepliesPerHour) {
        return aiRateLimitService.allowByHourlyLimit(scene, botUserId, maxRepliesPerHour);
    }

    public boolean allowByDailyLimit(String scene, Long botUserId, Integer maxRepliesPerDay) {
        return aiRateLimitService.allowByDailyLimit(scene, botUserId, maxRepliesPerDay);
    }

    public boolean allowGlobalQuota(String scene, Long botUserId) {
        AiConfig.Guard guard = aiConfig.getGuard();
        if (guard == null || !Boolean.TRUE.equals(guard.getEnabled())) {
            return true;
        }
        if (!aiRateLimitService.allowByGlobalHourlyLimit(botUserId, guard.getMaxCallsPerHour())) {
            log.info("AI 全局小时配额已达上限: scene={}, botUserId={}", scene, botUserId);
            return false;
        }
        if (!aiRateLimitService.allowByGlobalDailyLimit(botUserId, guard.getMaxCallsPerDay())) {
            log.warn("AI 全局日配额已达上限: scene={}, botUserId={}", scene, botUserId);
            return false;
        }
        return true;
    }

    public int resolveMentionMinDelaySeconds(Integer mentionMinDelaySeconds, Integer defaultDelaySeconds) {
        int fallback = defaultDelaySeconds == null || defaultDelaySeconds <= 0 ? 4 : defaultDelaySeconds;
        if (mentionMinDelaySeconds == null || mentionMinDelaySeconds <= 0) {
            return Math.max(2, Math.min(fallback, 12));
        }
        return Math.max(1, Math.min(mentionMinDelaySeconds, 20));
    }

    public int resolveMentionMaxDelaySeconds(Integer mentionMaxDelaySeconds, Integer defaultDelaySeconds, int minDelaySeconds) {
        int fallback = defaultDelaySeconds == null || defaultDelaySeconds <= 0
                ? Math.max(minDelaySeconds + 2, 8)
                : defaultDelaySeconds;
        int resolved = mentionMaxDelaySeconds == null || mentionMaxDelaySeconds <= 0
                ? Math.max(minDelaySeconds + 2, Math.min(fallback, 30))
                : Math.max(mentionMaxDelaySeconds, minDelaySeconds);
        return Math.max(minDelaySeconds, Math.min(resolved, 40));
    }
}

