package com.wreckloud.wolfchat.ai.application.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.wreckloud.wolfchat.ai.domain.entity.WfAiSessionState;
import com.wreckloud.wolfchat.ai.infra.mapper.WfAiSessionStateMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import java.time.LocalDateTime;

/**
 * AI 会话状态服务。
 */
@Service
@RequiredArgsConstructor
public class AiSessionStateService {
    private static final int DEFAULT_VALUE = 50;

    private final WfAiSessionStateMapper wfAiSessionStateMapper;

    public void upsertState(String scene,
                            String sessionKey,
                            Long botUserId,
                            Long userId,
                            String lastUserMessage,
                            String lastAiReply) {
        if (!StringUtils.hasText(scene) || !StringUtils.hasText(sessionKey) || botUserId == null || botUserId <= 0L) {
            return;
        }
        WfAiSessionState state = findBySceneAndSessionKey(scene, sessionKey);
        LocalDateTime now = LocalDateTime.now();
        if (state == null) {
            state = new WfAiSessionState();
            state.setScene(scene.trim());
            state.setSessionKey(sessionKey.trim());
            state.setBotUserId(botUserId);
            state.setMessageCount(0);
            state.setWarmth(DEFAULT_VALUE);
            state.setEnergy(DEFAULT_VALUE);
            state.setPatience(DEFAULT_VALUE);
        }
        state.setUserId(userId);
        state.setMood(resolveMood(lastUserMessage, lastAiReply));
        state.setWarmth(resolveWarmth(lastUserMessage, state.getWarmth()));
        state.setEnergy(resolveEnergy(lastAiReply, state.getEnergy()));
        state.setPatience(resolvePatience(lastUserMessage, state.getPatience()));
        state.setTopic(extractTopic(lastUserMessage));
        state.setLastUserMessage(truncate(lastUserMessage, 300));
        state.setLastAiReply(truncate(lastAiReply, 300));
        state.setMessageCount((state.getMessageCount() == null ? 0 : state.getMessageCount()) + 1);
        state.setLastTouchedAt(now);
        if (state.getId() == null) {
            wfAiSessionStateMapper.insert(state);
            return;
        }
        wfAiSessionStateMapper.updateById(state);
    }

    public String buildStateDigest(String scene, String sessionKey) {
        WfAiSessionState state = findBySceneAndSessionKey(scene, sessionKey);
        if (state == null) {
            return null;
        }
        StringBuilder builder = new StringBuilder("当前会话状态：");
        if (StringUtils.hasText(state.getMood())) {
            builder.append("mood=").append(state.getMood()).append("；");
        }
        if (state.getWarmth() != null) {
            builder.append("warmth=").append(state.getWarmth()).append("；");
        }
        if (state.getEnergy() != null) {
            builder.append("energy=").append(state.getEnergy()).append("；");
        }
        if (state.getPatience() != null) {
            builder.append("patience=").append(state.getPatience()).append("；");
        }
        if (StringUtils.hasText(state.getTopic())) {
            builder.append("topic=").append(state.getTopic()).append("。");
        }
        return builder.toString();
    }

    public String resolvePrivateEngagementMode(String scene, String sessionKey, String latestUserMessage) {
        String userText = safeText(latestUserMessage);
        if (isGreeting(userText)) {
            return "greeting";
        }
        if (isBanter(userText)) {
            return "banter";
        }
        WfAiSessionState state = findBySceneAndSessionKey(scene, sessionKey);
        if (state != null && "playful".equalsIgnoreCase(state.getMood()) && isLowSignalText(userText)) {
            return "banter";
        }
        return "serious";
    }

    public boolean isPrivateConversationStalled(String scene, String sessionKey, String latestUserMessage) {
        WfAiSessionState state = findBySceneAndSessionKey(scene, sessionKey);
        if (state == null) {
            return false;
        }
        String latestUser = safeText(latestUserMessage);
        String previousUser = safeText(state.getLastUserMessage());
        String previousAi = safeText(state.getLastAiReply());
        boolean lowSignalPair = isLowSignalText(latestUser) && isLowSignalText(previousUser);
        boolean aiDryReply = isLowSignalText(previousAi) || previousAi.length() < 10;
        return lowSignalPair || (aiDryReply && isLowSignalText(latestUser));
    }

    private WfAiSessionState findBySceneAndSessionKey(String scene, String sessionKey) {
        if (!StringUtils.hasText(scene) || !StringUtils.hasText(sessionKey)) {
            return null;
        }
        LambdaQueryWrapper<WfAiSessionState> queryWrapper = new LambdaQueryWrapper<>();
        queryWrapper.eq(WfAiSessionState::getScene, scene.trim())
                .eq(WfAiSessionState::getSessionKey, sessionKey.trim())
                .last("LIMIT 1");
        return wfAiSessionStateMapper.selectOne(queryWrapper);
    }

    private String resolveMood(String userMessage, String aiReply) {
        String text = (safeText(userMessage) + " " + safeText(aiReply)).toLowerCase();
        if (text.contains("气") || text.contains("怒") || text.contains("烦")) {
            return "irritated";
        }
        if (text.contains("哈哈") || text.contains("笑") || text.contains("乐")) {
            return "playful";
        }
        if (text.contains("忙") || text.contains("赶") || text.contains("急")) {
            return "focused";
        }
        return "steady";
    }

    private boolean isGreeting(String text) {
        String lower = safeText(text).toLowerCase();
        if (!StringUtils.hasText(lower)) {
            return false;
        }
        return lower.contains("你好")
                || lower.contains("哈喽")
                || lower.contains("嗨")
                || lower.contains("在吗")
                || lower.contains("有人吗")
                || lower.contains("还在吗")
                || lower.contains("在不在");
    }

    private boolean isBanter(String text) {
        String lower = safeText(text).toLowerCase();
        if (!StringUtils.hasText(lower)) {
            return false;
        }
        return lower.contains("哈哈")
                || lower.contains("笑")
                || lower.contains("乐")
                || lower.contains("梗")
                || lower.contains("6")
                || lower.contains("离谱")
                || lower.contains("抽象");
    }

    private boolean isLowSignalText(String text) {
        String normalized = safeText(text).toLowerCase();
        if (!StringUtils.hasText(normalized)) {
            return true;
        }
        if (normalized.length() <= 2) {
            return true;
        }
        return "在".equals(normalized)
                || "在呢".equals(normalized)
                || "嗯".equals(normalized)
                || "嗯嗯".equals(normalized)
                || "哦".equals(normalized)
                || "好的".equals(normalized)
                || "...".equals(normalized)
                || "。。。".equals(normalized);
    }

    private Integer resolveWarmth(String userMessage, Integer previous) {
        int base = previous == null ? DEFAULT_VALUE : previous;
        String text = safeText(userMessage);
        if (text.contains("谢谢") || text.contains("辛苦")) {
            return clamp(base + 6);
        }
        if (text.contains("滚") || text.contains("烦") || text.contains("闭嘴")) {
            return clamp(base - 8);
        }
        return clamp(base);
    }

    private Integer resolveEnergy(String aiReply, Integer previous) {
        int base = previous == null ? DEFAULT_VALUE : previous;
        String text = safeText(aiReply);
        if (text.length() > 60) {
            return clamp(base + 4);
        }
        if (text.length() < 12) {
            return clamp(base - 3);
        }
        return clamp(base);
    }

    private Integer resolvePatience(String userMessage, Integer previous) {
        int base = previous == null ? DEFAULT_VALUE : previous;
        String text = safeText(userMessage);
        if (text.contains("为什么") || text.contains("咋")) {
            return clamp(base + 2);
        }
        if (text.contains("快点") || text.contains("赶紧")) {
            return clamp(base - 4);
        }
        return clamp(base);
    }

    private String extractTopic(String userMessage) {
        String text = safeText(userMessage);
        if (!StringUtils.hasText(text)) {
            return null;
        }
        return truncate(text, 28);
    }

    private int clamp(int value) {
        return Math.max(0, Math.min(value, 100));
    }

    private String safeText(String text) {
        return text == null ? "" : text.trim();
    }

    private String truncate(String value, int maxChars) {
        if (!StringUtils.hasText(value)) {
            return null;
        }
        String normalized = value.replace('\n', ' ').replace('\r', ' ').trim();
        if (normalized.length() <= maxChars) {
            return normalized;
        }
        return normalized.substring(0, maxChars) + "...";
    }
}
