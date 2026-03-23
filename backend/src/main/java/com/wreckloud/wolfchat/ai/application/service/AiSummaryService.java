package com.wreckloud.wolfchat.ai.application.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.wreckloud.wolfchat.ai.config.AiConfig;
import com.wreckloud.wolfchat.ai.domain.entity.WfAiSessionSummary;
import com.wreckloud.wolfchat.ai.infra.mapper.WfAiSessionSummaryMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import java.time.LocalDateTime;
import java.util.List;

/**
 * AI 会话摘要服务（MySQL 持久化）。
 */
@Service
@RequiredArgsConstructor
public class AiSummaryService {
    private static final int DEFAULT_TRIGGER_COUNT = 16;
    private static final int DEFAULT_SUMMARY_MAX_CHARS = 360;

    private final AiConfig aiConfig;
    private final WfAiSessionSummaryMapper wfAiSessionSummaryMapper;

    public String getSummaryDigest(String scene, String sessionKey) {
        WfAiSessionSummary summary = findSummary(scene, sessionKey);
        if (summary == null || !StringUtils.hasText(summary.getSummaryText())) {
            return null;
        }
        return "最近摘要：" + summary.getSummaryText().trim();
    }

    public void refreshSummary(String scene, String sessionKey, Long botUserId, List<String> lines) {
        if (!StringUtils.hasText(scene) || !StringUtils.hasText(sessionKey) || botUserId == null || botUserId <= 0L) {
            return;
        }
        WfAiSessionSummary summary = findSummary(scene, sessionKey);
        if (summary == null) {
            summary = new WfAiSessionSummary();
            summary.setScene(scene.trim());
            summary.setSessionKey(sessionKey.trim());
            summary.setBotUserId(botUserId);
            summary.setMessageCount(0);
        }
        int messageCount = (summary.getMessageCount() == null ? 0 : summary.getMessageCount()) + 1;
        summary.setMessageCount(messageCount);
        if (shouldUpdateSummary(messageCount)) {
            summary.setSummaryText(buildSummaryText(lines));
            summary.setLastSummarizedAt(LocalDateTime.now());
        }
        if (summary.getId() == null) {
            wfAiSessionSummaryMapper.insert(summary);
            return;
        }
        wfAiSessionSummaryMapper.updateById(summary);
    }

    private boolean shouldUpdateSummary(int messageCount) {
        int triggerCount = resolveTriggerCount();
        return messageCount == 1 || messageCount % triggerCount == 0;
    }

    private String buildSummaryText(List<String> lines) {
        if (lines == null || lines.isEmpty()) {
            return "暂无有效摘要。";
        }
        String merged = String.join(" | ", lines).replace('\n', ' ').replace('\r', ' ').trim();
        if (!StringUtils.hasText(merged)) {
            return "暂无有效摘要。";
        }
        int maxChars = resolveSummaryMaxChars();
        if (merged.length() <= maxChars) {
            return merged;
        }
        return merged.substring(0, maxChars) + "...";
    }

    private int resolveTriggerCount() {
        Integer configured = aiConfig.getMemory() == null ? null : aiConfig.getMemory().getSummaryTriggerMessageCount();
        if (configured == null || configured <= 0) {
            return DEFAULT_TRIGGER_COUNT;
        }
        return Math.max(8, Math.min(configured, 120));
    }

    private int resolveSummaryMaxChars() {
        Integer configuredTokens = aiConfig.getMemory() == null ? null : aiConfig.getMemory().getSummaryMaxTokens();
        if (configuredTokens == null || configuredTokens <= 0) {
            return DEFAULT_SUMMARY_MAX_CHARS;
        }
        int charBudget = configuredTokens * 2;
        return Math.max(120, Math.min(charBudget, 1200));
    }

    private WfAiSessionSummary findSummary(String scene, String sessionKey) {
        if (!StringUtils.hasText(scene) || !StringUtils.hasText(sessionKey)) {
            return null;
        }
        LambdaQueryWrapper<WfAiSessionSummary> queryWrapper = new LambdaQueryWrapper<>();
        queryWrapper.eq(WfAiSessionSummary::getScene, scene.trim())
                .eq(WfAiSessionSummary::getSessionKey, sessionKey.trim())
                .last("LIMIT 1");
        return wfAiSessionSummaryMapper.selectOne(queryWrapper);
    }
}
