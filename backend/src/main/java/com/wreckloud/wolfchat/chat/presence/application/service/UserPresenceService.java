package com.wreckloud.wolfchat.chat.presence.application.service;

import com.wreckloud.wolfchat.chat.conversation.api.vo.ConversationVO;
import lombok.RequiredArgsConstructor;
import lombok.Value;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.ZSetOperations;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Set;

/**
 * @Description 用户在线状态服务
 * @Author Wreckloud
 * @Date 2026-03-07
 */
@Service
@RequiredArgsConstructor
public class UserPresenceService {
    private static final String ONLINE_ZSET_KEY = "wolfchat:presence:online";
    private static final String ACTIVE_ZSET_KEY = "wolfchat:presence:active";
    private static final long ONLINE_TTL_SECONDS = 120L;
    private static final long ONLINE_TTL_MILLIS = ONLINE_TTL_SECONDS * 1000L;

    private final StringRedisTemplate stringRedisTemplate;

    public boolean markOnline(Long userId) {
        if (userId == null) {
            return false;
        }
        long nowMillis = System.currentTimeMillis();
        long cutoff = nowMillis - ONLINE_TTL_MILLIS;

        ZSetOperations<String, String> zSetOps = stringRedisTemplate.opsForZSet();
        cleanupExpiredOnline(cutoff, zSetOps);

        String member = String.valueOf(userId);
        Double previousScore = zSetOps.score(ONLINE_ZSET_KEY, member);
        zSetOps.add(ONLINE_ZSET_KEY, member, nowMillis);
        zSetOps.add(ACTIVE_ZSET_KEY, member, nowMillis);
        return previousScore == null;
    }

    public LocalDateTime markOffline(Long userId) {
        if (userId == null) {
            return null;
        }
        String member = String.valueOf(userId);
        Long removed = stringRedisTemplate.opsForZSet().remove(ONLINE_ZSET_KEY, member);
        if (removed == null || removed <= 0) {
            return null;
        }

        LocalDateTime lastSeenAt = LocalDateTime.now();
        long now = lastSeenAt.atZone(ZoneId.systemDefault()).toInstant().toEpochMilli();
        stringRedisTemplate.opsForZSet().add(ACTIVE_ZSET_KEY, member, now);
        return lastSeenAt;
    }

    public void fillConversationPresence(List<ConversationVO> conversationList) {
        if (conversationList == null || conversationList.isEmpty()) {
            return;
        }

        long nowMillis = System.currentTimeMillis();
        long cutoff = nowMillis - ONLINE_TTL_MILLIS;
        ZSetOperations<String, String> zSetOps = stringRedisTemplate.opsForZSet();
        cleanupExpiredOnline(cutoff, zSetOps);

        for (ConversationVO conversation : conversationList) {
            Long targetUserId = conversation.getTargetUserId();
            if (targetUserId == null) {
                conversation.setIsOnline(false);
                conversation.setLastSeenAt(null);
                continue;
            }
            String member = String.valueOf(targetUserId);
            Double onlineScore = zSetOps.score(ONLINE_ZSET_KEY, member);
            boolean online = onlineScore != null && onlineScore.longValue() >= cutoff;
            conversation.setIsOnline(online);
            if (!online) {
                Double activeScore = zSetOps.score(ACTIVE_ZSET_KEY, member);
                conversation.setLastSeenAt(parseEpochMillis(activeScore));
            }
        }
    }

    public int countOnlineUsers() {
        long nowMillis = System.currentTimeMillis();
        long cutoff = nowMillis - ONLINE_TTL_MILLIS;
        ZSetOperations<String, String> zSetOps = stringRedisTemplate.opsForZSet();
        cleanupExpiredOnline(cutoff, zSetOps);
        Long count = zSetOps.count(ONLINE_ZSET_KEY, cutoff, Double.POSITIVE_INFINITY);
        if (count == null) {
            return 0;
        }
        return count.intValue();
    }

    public List<PresenceSnapshot> listRecentActiveUsers(int limit) {
        if (limit <= 0) {
            return Collections.emptyList();
        }

        long nowMillis = System.currentTimeMillis();
        long cutoff = nowMillis - ONLINE_TTL_MILLIS;
        ZSetOperations<String, String> zSetOps = stringRedisTemplate.opsForZSet();
        cleanupExpiredOnline(cutoff, zSetOps);

        Set<ZSetOperations.TypedTuple<String>> activeTuples = zSetOps
                .reverseRangeWithScores(ACTIVE_ZSET_KEY, 0, limit - 1L);
        if (activeTuples == null || activeTuples.isEmpty()) {
            return Collections.emptyList();
        }

        List<PresenceSnapshot> snapshots = new ArrayList<>(activeTuples.size());
        for (ZSetOperations.TypedTuple<String> tuple : activeTuples) {
            String rawUserId = tuple.getValue();
            Long userId = parseUserId(rawUserId);
            if (userId == null) {
                continue;
            }
            Double activeScore = tuple.getScore();
            LocalDateTime lastActiveAt = parseEpochMillis(activeScore);
            if (lastActiveAt == null) {
                continue;
            }
            Double onlineScore = zSetOps.score(ONLINE_ZSET_KEY, rawUserId);
            boolean online = onlineScore != null && onlineScore.longValue() >= cutoff;
            snapshots.add(new PresenceSnapshot(userId, online, lastActiveAt));
        }
        return snapshots;
    }

    private void cleanupExpiredOnline(long cutoff, ZSetOperations<String, String> zSetOps) {
        zSetOps.removeRangeByScore(ONLINE_ZSET_KEY, Double.NEGATIVE_INFINITY, cutoff - 1);
    }

    private Long parseUserId(String rawUserId) {
        if (!StringUtils.hasText(rawUserId)) {
            return null;
        }
        try {
            return Long.parseLong(rawUserId);
        } catch (NumberFormatException ex) {
            return null;
        }
    }

    private LocalDateTime parseEpochMillis(Double score) {
        if (score == null) {
            return null;
        }
        long epochMillis = score.longValue();
        return LocalDateTime.ofInstant(Instant.ofEpochMilli(epochMillis), ZoneId.systemDefault());
    }

    @Value
    public static class PresenceSnapshot {
        Long userId;
        boolean online;
        LocalDateTime lastActiveAt;
    }
}
