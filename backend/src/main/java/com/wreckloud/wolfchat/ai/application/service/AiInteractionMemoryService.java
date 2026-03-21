package com.wreckloud.wolfchat.ai.application.service;

import lombok.RequiredArgsConstructor;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import java.time.Duration;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * AI 互动记忆服务（轻量 Redis 持久化）。
 */
@Service
@RequiredArgsConstructor
public class AiInteractionMemoryService {
    private static final String USER_LIST_KEY_PREFIX = "ai:memory:users:";
    private static final String TOPIC_LIST_KEY_PREFIX = "ai:memory:topics:";
    private static final int MAX_USER_WINDOW = 30;
    private static final int MAX_TOPIC_WINDOW = 20;
    private static final int MAX_TOPIC_CHARS = 48;
    private static final Duration MEMORY_TTL = Duration.ofDays(14);

    private final StringRedisTemplate stringRedisTemplate;

    public void recordInteraction(String scene, Long botUserId, Long humanUserId, String text) {
        if (!StringUtils.hasText(scene) || !isPositive(botUserId) || !isPositive(humanUserId)) {
            return;
        }
        String userKey = buildUserKey(scene, botUserId);
        String humanIdText = String.valueOf(humanUserId);
        stringRedisTemplate.opsForList().remove(userKey, 0, humanIdText);
        stringRedisTemplate.opsForList().leftPush(userKey, humanIdText);
        stringRedisTemplate.opsForList().trim(userKey, 0, MAX_USER_WINDOW - 1L);
        stringRedisTemplate.expire(userKey, MEMORY_TTL);

        String topic = normalizeTopic(text);
        if (!StringUtils.hasText(topic)) {
            return;
        }
        String topicKey = buildTopicKey(scene, botUserId);
        stringRedisTemplate.opsForList().remove(topicKey, 0, topic);
        stringRedisTemplate.opsForList().leftPush(topicKey, topic);
        stringRedisTemplate.opsForList().trim(topicKey, 0, MAX_TOPIC_WINDOW - 1L);
        stringRedisTemplate.expire(topicKey, MEMORY_TTL);
    }

    public List<Long> listRecentUserIds(String scene, Long botUserId, int limit) {
        if (!StringUtils.hasText(scene) || !isPositive(botUserId) || limit <= 0) {
            return Collections.emptyList();
        }
        List<String> raw = stringRedisTemplate.opsForList().range(buildUserKey(scene, botUserId), 0, limit - 1L);
        if (raw == null || raw.isEmpty()) {
            return Collections.emptyList();
        }
        List<Long> result = new ArrayList<>(raw.size());
        for (String item : raw) {
            if (!StringUtils.hasText(item)) {
                continue;
            }
            try {
                long value = Long.parseLong(item.trim());
                if (value > 0L) {
                    result.add(value);
                }
            } catch (NumberFormatException ignored) {
                // ignore malformed entry
            }
        }
        return result;
    }

    public String buildTopicDigest(String scene, Long botUserId, int limit) {
        if (!StringUtils.hasText(scene) || !isPositive(botUserId) || limit <= 0) {
            return null;
        }
        List<String> topics = stringRedisTemplate.opsForList().range(buildTopicKey(scene, botUserId), 0, limit - 1L);
        if (topics == null || topics.isEmpty()) {
            return null;
        }
        List<String> normalized = new ArrayList<>();
        for (String topic : topics) {
            if (StringUtils.hasText(topic)) {
                normalized.add(topic.trim());
            }
        }
        if (normalized.isEmpty()) {
            return null;
        }
        return "近期互动话题：" + String.join(" | ", normalized);
    }

    private String normalizeTopic(String text) {
        if (!StringUtils.hasText(text)) {
            return null;
        }
        String normalized = text.replace('\n', ' ').replace('\r', ' ').trim();
        if (!StringUtils.hasText(normalized)) {
            return null;
        }
        if (normalized.length() <= MAX_TOPIC_CHARS) {
            return normalized;
        }
        return normalized.substring(0, MAX_TOPIC_CHARS) + "...";
    }

    private String buildUserKey(String scene, Long botUserId) {
        return USER_LIST_KEY_PREFIX + scene.trim() + ":" + botUserId;
    }

    private String buildTopicKey(String scene, Long botUserId) {
        return TOPIC_LIST_KEY_PREFIX + scene.trim() + ":" + botUserId;
    }

    private boolean isPositive(Long value) {
        return value != null && value > 0L;
    }
}
