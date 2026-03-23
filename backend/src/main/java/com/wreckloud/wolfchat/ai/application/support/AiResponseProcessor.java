package com.wreckloud.wolfchat.ai.application.support;

import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

/**
 * @Description AI 回复后处理器
 * @Author Wreckloud
 * @Date 2026-03-23
 */
@Component
public class AiResponseProcessor {
    private static final String[] BANNED_PHRASES = {
            "作为ai",
            "作为一个ai",
            "我是ai",
            "作为助手",
            "我是助手",
            "您好，",
            "您好!"
    };

    public String processPrivateReply(String rawReply, Integer configuredMaxChars, int defaultMaxChars) {
        return processByScene(rawReply, configuredMaxChars, defaultMaxChars, "行，我知道了。");
    }

    public String processLobbyReply(String rawReply, Integer configuredMaxChars, int defaultMaxChars) {
        return processByScene(rawReply, configuredMaxChars, defaultMaxChars, "这话有点意思。");
    }

    public String processForumReply(String rawReply, Integer configuredMaxChars, int defaultMaxChars) {
        return processByScene(rawReply, configuredMaxChars, defaultMaxChars, "这个点我认同一半。");
    }

    private String processByScene(String rawReply,
                                  Integer configuredMaxChars,
                                  int defaultMaxChars,
                                  String fallbackReply) {
        if (!StringUtils.hasText(rawReply)) {
            return fallbackReply;
        }
        String normalized = rawReply.replace("\r", "").trim();
        if (!StringUtils.hasText(normalized)) {
            return fallbackReply;
        }

        String lower = normalized.toLowerCase();
        for (String phrase : BANNED_PHRASES) {
            if (lower.contains(phrase)) {
                return fallbackReply;
            }
        }

        normalized = normalized
                .replace("首先，", "")
                .replace("其次，", "")
                .replace("最后，", "")
                .replace("总的来说，", "")
                .trim();
        if (!StringUtils.hasText(normalized)) {
            return fallbackReply;
        }

        int maxChars = resolveMaxReplyChars(configuredMaxChars, defaultMaxChars);
        if (normalized.length() <= maxChars) {
            return normalized;
        }
        return normalized.substring(0, maxChars).trim();
    }

    private int resolveMaxReplyChars(Integer configuredMaxChars, int defaultMaxChars) {
        int safeDefault = defaultMaxChars <= 0 ? 180 : defaultMaxChars;
        if (configuredMaxChars == null || configuredMaxChars <= 0) {
            return safeDefault;
        }
        return Math.max(60, Math.min(configuredMaxChars, 600));
    }
}

