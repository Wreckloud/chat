package com.wreckloud.wolfchat.ai.application.service;

import com.alibaba.fastjson.JSON;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.wreckloud.wolfchat.chat.lobby.domain.entity.WfLobbyMessage;
import com.wreckloud.wolfchat.chat.lobby.infra.mapper.WfLobbyMessageMapper;
import com.wreckloud.wolfchat.chat.message.domain.entity.WfMessage;
import com.wreckloud.wolfchat.chat.message.domain.enums.MessageType;
import com.wreckloud.wolfchat.chat.message.infra.mapper.WfMessageMapper;
import com.wreckloud.wolfchat.community.domain.entity.WfForumReply;
import com.wreckloud.wolfchat.community.domain.enums.ForumReplyStatus;
import com.wreckloud.wolfchat.community.infra.mapper.WfForumReplyMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import java.time.Duration;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * 最近消息窗口服务（Redis 缓存 + MySQL 回补）。
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class RecentMessageService {
    private static final int DEFAULT_LIMIT = 20;
    private static final int MAX_LIMIT = 50;
    private static final int MIN_LIMIT = 8;
    private static final Duration CACHE_TTL = Duration.ofSeconds(20);
    private static final String PRIVATE_KEY_PREFIX = "ai:recent:private:";
    private static final String LOBBY_KEY_PREFIX = "ai:recent:lobby:";
    private static final String FORUM_KEY_PREFIX = "ai:recent:forum:";

    private final StringRedisTemplate stringRedisTemplate;
    private final WfMessageMapper wfMessageMapper;
    private final WfLobbyMessageMapper wfLobbyMessageMapper;
    private final WfForumReplyMapper wfForumReplyMapper;

    public List<WfMessage> loadRecentPrivateMessages(Long conversationId, int configuredLimit) {
        if (conversationId == null || conversationId <= 0L) {
            return Collections.emptyList();
        }
        int limit = resolveLimit(configuredLimit);
        String cacheKey = PRIVATE_KEY_PREFIX + conversationId + ":" + limit;
        List<WfMessage> cached = readCacheList(cacheKey, WfMessage.class);
        if (!cached.isEmpty()) {
            log.debug("AI 最近私聊上下文命中缓存: conversationId={}, limit={}", conversationId, limit);
            return cached;
        }
        LambdaQueryWrapper<WfMessage> queryWrapper = new LambdaQueryWrapper<>();
        queryWrapper.eq(WfMessage::getConversationId, conversationId)
                .notIn(WfMessage::getMsgType, List.of(MessageType.SYSTEM, MessageType.RECALL))
                .orderByDesc(WfMessage::getCreateTime)
                .orderByDesc(WfMessage::getId)
                .last("LIMIT " + limit);
        List<WfMessage> rows = wfMessageMapper.selectList(queryWrapper);
        Collections.reverse(rows);
        writeCacheList(cacheKey, rows);
        log.debug("AI 最近私聊上下文走DB回补: conversationId={}, limit={}, size={}", conversationId, limit, rows.size());
        return rows;
    }

    public List<WfLobbyMessage> loadRecentLobbyMessages(int configuredLimit) {
        int limit = resolveLimit(configuredLimit);
        String cacheKey = LOBBY_KEY_PREFIX + limit;
        List<WfLobbyMessage> cached = readCacheList(cacheKey, WfLobbyMessage.class);
        if (!cached.isEmpty()) {
            log.debug("AI 最近大厅上下文命中缓存: limit={}", limit);
            return cached;
        }
        LambdaQueryWrapper<WfLobbyMessage> queryWrapper = new LambdaQueryWrapper<>();
        queryWrapper.notIn(WfLobbyMessage::getMsgType, List.of(MessageType.RECALL))
                .orderByDesc(WfLobbyMessage::getCreateTime)
                .orderByDesc(WfLobbyMessage::getId)
                .last("LIMIT " + limit);
        List<WfLobbyMessage> rows = wfLobbyMessageMapper.selectList(queryWrapper);
        Collections.reverse(rows);
        writeCacheList(cacheKey, rows);
        log.debug("AI 最近大厅上下文走DB回补: limit={}, size={}", limit, rows.size());
        return rows;
    }

    public List<WfForumReply> loadRecentForumReplies(Long threadId, int configuredLimit) {
        if (threadId == null || threadId <= 0L) {
            return Collections.emptyList();
        }
        int limit = resolveLimit(configuredLimit);
        String cacheKey = FORUM_KEY_PREFIX + threadId + ":" + limit;
        List<WfForumReply> cached = readCacheList(cacheKey, WfForumReply.class);
        if (!cached.isEmpty()) {
            log.debug("AI 最近论坛上下文命中缓存: threadId={}, limit={}", threadId, limit);
            return cached;
        }
        LambdaQueryWrapper<WfForumReply> queryWrapper = new LambdaQueryWrapper<>();
        queryWrapper.eq(WfForumReply::getThreadId, threadId)
                .eq(WfForumReply::getStatus, ForumReplyStatus.NORMAL)
                .orderByDesc(WfForumReply::getCreateTime)
                .orderByDesc(WfForumReply::getId)
                .last("LIMIT " + limit);
        List<WfForumReply> rows = wfForumReplyMapper.selectList(queryWrapper);
        Collections.reverse(rows);
        writeCacheList(cacheKey, rows);
        log.debug("AI 最近论坛上下文走DB回补: threadId={}, limit={}, size={}", threadId, limit, rows.size());
        return rows;
    }

    public void invalidatePrivateConversationCache(Long conversationId) {
        if (conversationId == null || conversationId <= 0L) {
            return;
        }
        List<String> keys = new ArrayList<>();
        for (int limit = MIN_LIMIT; limit <= MAX_LIMIT; limit++) {
            keys.add(PRIVATE_KEY_PREFIX + conversationId + ":" + limit);
        }
        deleteCacheKeys(keys, "private", String.valueOf(conversationId));
    }

    public void invalidateLobbyCache() {
        List<String> keys = new ArrayList<>();
        for (int limit = MIN_LIMIT; limit <= MAX_LIMIT; limit++) {
            keys.add(LOBBY_KEY_PREFIX + limit);
        }
        deleteCacheKeys(keys, "lobby", "global");
    }

    public void invalidateForumThreadCache(Long threadId) {
        if (threadId == null || threadId <= 0L) {
            return;
        }
        List<String> keys = new ArrayList<>();
        for (int limit = MIN_LIMIT; limit <= MAX_LIMIT; limit++) {
            keys.add(FORUM_KEY_PREFIX + threadId + ":" + limit);
        }
        deleteCacheKeys(keys, "forum", String.valueOf(threadId));
    }

    private int resolveLimit(int configuredLimit) {
        if (configuredLimit <= 0) {
            return DEFAULT_LIMIT;
        }
        return Math.max(MIN_LIMIT, Math.min(configuredLimit, MAX_LIMIT));
    }

    private <T> List<T> readCacheList(String cacheKey, Class<T> itemClass) {
        if (!StringUtils.hasText(cacheKey)) {
            return Collections.emptyList();
        }
        String payload;
        try {
            payload = stringRedisTemplate.opsForValue().get(cacheKey);
        } catch (Exception ex) {
            log.warn("AI 最近消息缓存读取失败，降级DB: key={}, message={}", cacheKey, ex.getMessage());
            return Collections.emptyList();
        }
        if (!StringUtils.hasText(payload)) {
            return Collections.emptyList();
        }
        try {
            List<T> rows = JSON.parseArray(payload, itemClass);
            return rows == null ? Collections.emptyList() : rows;
        } catch (Exception ignored) {
            return Collections.emptyList();
        }
    }

    private <T> void writeCacheList(String cacheKey, List<T> rows) {
        if (!StringUtils.hasText(cacheKey) || rows == null || rows.isEmpty()) {
            return;
        }
        try {
            stringRedisTemplate.opsForValue().set(cacheKey, JSON.toJSONString(rows), CACHE_TTL);
        } catch (Exception ex) {
            log.warn("AI 最近消息缓存写入失败，忽略缓存: key={}, message={}", cacheKey, ex.getMessage());
        }
    }

    private void deleteCacheKeys(List<String> keys, String scene, String target) {
        if (keys == null || keys.isEmpty()) {
            return;
        }
        try {
            Long deleted = stringRedisTemplate.delete(keys);
            log.debug("AI 最近上下文缓存失效: scene={}, target={}, deleted={}", scene, target, deleted == null ? 0 : deleted);
        } catch (Exception ex) {
            log.warn("AI 最近上下文缓存失效失败: scene={}, target={}, message={}", scene, target, ex.getMessage());
        }
    }
}
