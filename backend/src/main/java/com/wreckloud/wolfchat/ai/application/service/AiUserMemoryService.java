package com.wreckloud.wolfchat.ai.application.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.wreckloud.wolfchat.ai.domain.entity.WfAiUserMemoryFact;
import com.wreckloud.wolfchat.ai.infra.mapper.WfAiUserMemoryFactMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * AI 用户长期记忆服务（MySQL 持久化）。
 */
@Service
@RequiredArgsConstructor
public class AiUserMemoryService {
    private static final int MAX_FACT_VALUE_CHARS = 160;

    private final WfAiUserMemoryFactMapper wfAiUserMemoryFactMapper;

    public void updateFacts(Long botUserId, Long userId, String scene, String... texts) {
        if (!isValidUserId(botUserId) || !isValidUserId(userId)) {
            return;
        }
        Map<String, String> extracted = extractFacts(texts);
        if (extracted.isEmpty()) {
            return;
        }
        for (Map.Entry<String, String> entry : extracted.entrySet()) {
            upsertFact(botUserId, userId, entry.getKey(), entry.getValue(), scene);
        }
    }

    public String buildMemoryDigest(Long botUserId, Long userId, int maxFacts) {
        if (!isValidUserId(botUserId) || !isValidUserId(userId) || maxFacts <= 0) {
            return null;
        }
        LambdaQueryWrapper<WfAiUserMemoryFact> queryWrapper = new LambdaQueryWrapper<>();
        queryWrapper.eq(WfAiUserMemoryFact::getBotUserId, botUserId)
                .eq(WfAiUserMemoryFact::getUserId, userId)
                .orderByDesc(WfAiUserMemoryFact::getLastSeenAt)
                .last("LIMIT " + maxFacts);
        List<WfAiUserMemoryFact> rows = wfAiUserMemoryFactMapper.selectList(queryWrapper);
        if (rows == null || rows.isEmpty()) {
            return null;
        }
        List<String> parts = new ArrayList<>();
        for (WfAiUserMemoryFact row : rows) {
            if (row == null || !StringUtils.hasText(row.getFactValue())) {
                continue;
            }
            parts.add(row.getFactKey() + "=" + row.getFactValue().trim());
        }
        if (parts.isEmpty()) {
            return null;
        }
        return "用户长期记忆：" + String.join("；", parts) + "。";
    }

    private void upsertFact(Long botUserId, Long userId, String factKey, String factValue, String scene) {
        if (!StringUtils.hasText(factKey) || !StringUtils.hasText(factValue)) {
            return;
        }
        LambdaQueryWrapper<WfAiUserMemoryFact> queryWrapper = new LambdaQueryWrapper<>();
        queryWrapper.eq(WfAiUserMemoryFact::getBotUserId, botUserId)
                .eq(WfAiUserMemoryFact::getUserId, userId)
                .eq(WfAiUserMemoryFact::getFactKey, factKey.trim())
                .last("LIMIT 1");
        WfAiUserMemoryFact existed = wfAiUserMemoryFactMapper.selectOne(queryWrapper);
        LocalDateTime now = LocalDateTime.now();
        if (existed == null) {
            WfAiUserMemoryFact fact = new WfAiUserMemoryFact();
            fact.setBotUserId(botUserId);
            fact.setUserId(userId);
            fact.setFactKey(factKey.trim());
            fact.setFactValue(truncate(factValue, MAX_FACT_VALUE_CHARS));
            fact.setConfidence(0.72D);
            fact.setSourceScene(scene);
            fact.setLastSeenAt(now);
            wfAiUserMemoryFactMapper.insert(fact);
            return;
        }
        existed.setFactValue(truncate(factValue, MAX_FACT_VALUE_CHARS));
        existed.setSourceScene(scene);
        existed.setConfidence(Math.min(0.98D, (existed.getConfidence() == null ? 0.7D : existed.getConfidence()) + 0.04D));
        existed.setLastSeenAt(now);
        wfAiUserMemoryFactMapper.updateById(existed);
    }

    private Map<String, String> extractFacts(String... texts) {
        Map<String, String> facts = new LinkedHashMap<>();
        if (texts == null) {
            return facts;
        }
        for (String text : texts) {
            if (!StringUtils.hasText(text)) {
                continue;
            }
            String normalized = normalize(text);
            String profile = extractByKeyword(normalized, "我是");
            if (StringUtils.hasText(profile)) {
                facts.put("profile", profile);
            }
            String preference = extractByKeyword(normalized, "喜欢");
            if (StringUtils.hasText(preference)) {
                facts.put("preference", preference);
            }
            String task = extractByKeyword(normalized, "最近在");
            if (!StringUtils.hasText(task)) {
                task = extractByKeyword(normalized, "正在");
            }
            if (StringUtils.hasText(task)) {
                facts.put("recent_task", task);
            }
        }
        return facts;
    }

    private String extractByKeyword(String text, String keyword) {
        if (!StringUtils.hasText(text) || !StringUtils.hasText(keyword)) {
            return null;
        }
        int start = text.indexOf(keyword);
        if (start < 0) {
            return null;
        }
        String tail = text.substring(start + keyword.length()).trim();
        if (!StringUtils.hasText(tail)) {
            return null;
        }
        int stop = findStopIndex(tail);
        String value = stop < 0 ? tail : tail.substring(0, stop);
        value = value.trim();
        if (!StringUtils.hasText(value)) {
            return null;
        }
        if (value.length() > MAX_FACT_VALUE_CHARS) {
            value = value.substring(0, MAX_FACT_VALUE_CHARS) + "...";
        }
        return value;
    }

    private int findStopIndex(String text) {
        int idx = -1;
        char[] stops = new char[]{'。', '！', '!', '？', '?', '，', ',', '；', ';', '\n'};
        for (char stop : stops) {
            int current = text.indexOf(stop);
            if (current >= 0) {
                idx = idx < 0 ? current : Math.min(idx, current);
            }
        }
        return idx;
    }

    private String normalize(String text) {
        return text.replace('\r', ' ').replace('\n', ' ').trim();
    }

    private String truncate(String value, int maxChars) {
        if (!StringUtils.hasText(value)) {
            return null;
        }
        String normalized = normalize(value);
        if (normalized.length() <= maxChars) {
            return normalized;
        }
        return normalized.substring(0, maxChars) + "...";
    }

    private boolean isValidUserId(Long userId) {
        return userId != null && userId > 0L;
    }
}
