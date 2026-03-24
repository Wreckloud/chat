package com.wreckloud.wolfchat.chat.presence.application.service;

import com.wreckloud.wolfchat.chat.conversation.api.vo.ConversationVO;
import lombok.RequiredArgsConstructor;
import lombok.Value;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.ZSetOperations;
import org.springframework.data.redis.core.RedisOperations;
import org.springframework.data.redis.core.SessionCallback;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
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

    public LocalDateTime markActive(Long userId) {
        if (userId == null) {
            return null;
        }
        LocalDateTime activeAt = LocalDateTime.now();
        long now = activeAt.atZone(ZoneId.systemDefault()).toInstant().toEpochMilli();
        stringRedisTemplate.opsForZSet().add(ACTIVE_ZSET_KEY, String.valueOf(userId), now);
        return activeAt;
    }

    public void fillConversationPresence(List<ConversationVO> conversationList) {
        if (conversationList == null || conversationList.isEmpty()) {
            return;
        }

        long nowMillis = System.currentTimeMillis();
        long cutoff = nowMillis - ONLINE_TTL_MILLIS;
        ZSetOperations<String, String> zSetOps = stringRedisTemplate.opsForZSet();
        cleanupExpiredOnline(cutoff, zSetOps);

        Set<String> members = new LinkedHashSet<>();
        for (ConversationVO conversation : conversationList) {
            Long targetUserId = conversation.getTargetUserId();
            if (targetUserId != null) {
                members.add(String.valueOf(targetUserId));
            }
        }
        Map<String, Double> onlineScoreMap = batchQueryScores(ONLINE_ZSET_KEY, members);
        Map<String, Double> activeScoreMap = batchQueryScores(ACTIVE_ZSET_KEY, members);

        for (ConversationVO conversation : conversationList) {
            Long targetUserId = conversation.getTargetUserId();
            if (targetUserId == null) {
                conversation.setIsOnline(false);
                conversation.setLastSeenAt(null);
                continue;
            }
            String member = String.valueOf(targetUserId);
            Double onlineScore = onlineScoreMap.get(member);
            boolean online = onlineScore != null && onlineScore.longValue() >= cutoff;
            conversation.setIsOnline(online);
            if (!online) {
                Double activeScore = activeScoreMap.get(member);
                LocalDateTime activeAt = parseEpochMillis(activeScore);
                if (activeAt != null) {
                    conversation.setLastSeenAt(activeAt);
                }
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

    public boolean isOnline(Long userId) {
        if (userId == null || userId <= 0L) {
            return false;
        }
        long nowMillis = System.currentTimeMillis();
        long cutoff = nowMillis - ONLINE_TTL_MILLIS;
        ZSetOperations<String, String> zSetOps = stringRedisTemplate.opsForZSet();
        cleanupExpiredOnline(cutoff, zSetOps);
        Double score = zSetOps.score(ONLINE_ZSET_KEY, String.valueOf(userId));
        return score != null && score.longValue() >= cutoff;
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

        Set<String> members = new LinkedHashSet<>();
        for (ZSetOperations.TypedTuple<String> tuple : activeTuples) {
            if (StringUtils.hasText(tuple.getValue())) {
                members.add(tuple.getValue());
            }
        }
        Map<String, Double> onlineScoreMap = batchQueryScores(ONLINE_ZSET_KEY, members);

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
            Double onlineScore = onlineScoreMap.get(rawUserId);
            boolean online = onlineScore != null && onlineScore.longValue() >= cutoff;
            snapshots.add(new PresenceSnapshot(userId, online, lastActiveAt));
        }
        return snapshots;
    }

    private void cleanupExpiredOnline(long cutoff, ZSetOperations<String, String> zSetOps) {
        zSetOps.removeRangeByScore(ONLINE_ZSET_KEY, Double.NEGATIVE_INFINITY, cutoff - 1);
    }

    private Map<String, Double> batchQueryScores(String key, Set<String> members) {
        if (members == null || members.isEmpty()) {
            return Collections.emptyMap();
        }

        List<String> memberList = new ArrayList<>(members);
        List<Object> scoreResults = stringRedisTemplate.executePipelined(new SessionCallback<Object>() {
            @Override
            public <K, V> Object execute(RedisOperations<K, V> operations) {
                @SuppressWarnings("unchecked")
                RedisOperations<String, String> stringOps = (RedisOperations<String, String>) operations;
                for (String member : memberList) {
                    stringOps.opsForZSet().score(key, member);
                }
                return null;
            }
        });

        Map<String, Double> scoreMap = new HashMap<>(memberList.size());
        if (scoreResults == null || scoreResults.isEmpty()) {
            return scoreMap;
        }
        int resultSize = Math.min(memberList.size(), scoreResults.size());
        for (int index = 0; index < resultSize; index++) {
            Object rawScore = scoreResults.get(index);
            if (!(rawScore instanceof Double)) {
                continue;
            }
            scoreMap.put(memberList.get(index), (Double) rawScore);
        }
        return scoreMap;
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
