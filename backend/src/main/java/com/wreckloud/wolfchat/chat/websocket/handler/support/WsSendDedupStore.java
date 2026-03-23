package com.wreckloud.wolfchat.chat.websocket.handler.support;

import com.wreckloud.wolfchat.chat.websocket.enums.WsType;
import lombok.RequiredArgsConstructor;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

import java.time.Duration;

/**
 * @Description WebSocket 发送幂等去重存储
 * @Author Wreckloud
 * @Date 2026-03-23
 */
@Component
@RequiredArgsConstructor
public class WsSendDedupStore {
    private static final String SEND_DEDUP_KEY_PREFIX = "chat:ws:send:dedup:";
    private static final Duration SEND_DEDUP_TTL = Duration.ofMinutes(10);
    private static final String SEND_DEDUP_PENDING = "PENDING";

    private final StringRedisTemplate stringRedisTemplate;

    public String buildKey(WsType wsType, Long userId, String clientMsgId) {
        if (!StringUtils.hasText(clientMsgId) || userId == null || wsType == null) {
            return null;
        }
        return SEND_DEDUP_KEY_PREFIX + wsType.name() + ":" + userId + ":" + clientMsgId.trim();
    }

    public Long getMessageId(String key) {
        if (!StringUtils.hasText(key)) {
            return null;
        }
        String raw = stringRedisTemplate.opsForValue().get(key);
        if (!StringUtils.hasText(raw) || SEND_DEDUP_PENDING.equals(raw)) {
            return null;
        }
        try {
            long parsed = Long.parseLong(raw);
            return parsed > 0 ? parsed : null;
        } catch (NumberFormatException ex) {
            return null;
        }
    }

    public boolean markPending(String key) {
        if (!StringUtils.hasText(key)) {
            return true;
        }
        Boolean created = stringRedisTemplate.opsForValue()
                .setIfAbsent(key, SEND_DEDUP_PENDING, SEND_DEDUP_TTL);
        return Boolean.TRUE.equals(created);
    }

    public void clearPending(String key) {
        if (!StringUtils.hasText(key)) {
            return;
        }
        String current = stringRedisTemplate.opsForValue().get(key);
        if (SEND_DEDUP_PENDING.equals(current)) {
            stringRedisTemplate.delete(key);
        }
    }

    public void storeMessageId(String key, Long messageId) {
        if (!StringUtils.hasText(key) || messageId == null || messageId <= 0L) {
            return;
        }
        stringRedisTemplate.opsForValue().set(key, String.valueOf(messageId), SEND_DEDUP_TTL);
    }

    public void delete(String key) {
        if (!StringUtils.hasText(key)) {
            return;
        }
        stringRedisTemplate.delete(key);
    }
}

