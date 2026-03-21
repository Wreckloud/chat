package com.wreckloud.wolfchat.ai.application.service;

import com.wreckloud.wolfchat.ai.config.AiConfig;
import lombok.RequiredArgsConstructor;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import java.time.Duration;

/**
 * AI 情绪状态服务（轻量、低成本，无需额外模型调用）。
 */
@Service
@RequiredArgsConstructor
public class AiMoodService {
    private static final String REDIS_PREFIX = "ai:mood:";
    private static final int MIN_MOOD_SCORE = -4;
    private static final int MAX_MOOD_SCORE = 4;

    private final StringRedisTemplate stringRedisTemplate;
    private final AiConfig aiConfig;

    public String buildMoodDirective(String scene, Long botUserId, String triggerText) {
        if (!Boolean.TRUE.equals(aiConfig.getEnabled()) || botUserId == null || botUserId <= 0L) {
            return null;
        }
        String moodKey = REDIS_PREFIX + scene + ":" + botUserId;
        int currentScore = parseScore(stringRedisTemplate.opsForValue().get(moodKey));
        int nextScore = clamp(currentScore + calcDelta(triggerText));
        stringRedisTemplate.opsForValue().set(moodKey, String.valueOf(nextScore), Duration.ofHours(12));
        return mapMoodDirective(nextScore);
    }

    private int calcDelta(String text) {
        if (!StringUtils.hasText(text)) {
            return 0;
        }
        String content = text.trim();
        int delta = 0;
        if (containsAny(content, "哈哈", "hh", "lol", "有趣", "牛", "好玩", "抽象")) {
            delta += 1;
        }
        if (containsAny(content, "?", "？", "怎么", "为啥", "真的假的")) {
            delta += 1;
        }
        if (containsAny(content, "烦", "烂", "傻", "离谱", "生气", "气死", "菜", "蠢", "逆天")) {
            delta -= 1;
        }
        if (containsAny(content, "!!!", "！！", "破防", "无语")) {
            delta -= 1;
        }
        return delta;
    }

    private boolean containsAny(String source, String... keywords) {
        for (String keyword : keywords) {
            if (source.contains(keyword)) {
                return true;
            }
        }
        return false;
    }

    private int parseScore(String scoreText) {
        if (!StringUtils.hasText(scoreText)) {
            return 0;
        }
        try {
            return clamp(Integer.parseInt(scoreText.trim()));
        } catch (NumberFormatException ignored) {
            return 0;
        }
    }

    private int clamp(int score) {
        return Math.max(MIN_MOOD_SCORE, Math.min(MAX_MOOD_SCORE, score));
    }

    private String mapMoodDirective(int score) {
        if (score <= -3) {
            return "当前情绪偏炸毛：语气更冷更短，可带轻微压迫感与锋利吐槽，但禁止失控辱骂。";
        }
        if (score <= -1) {
            return "当前情绪略不耐烦：保持克制前提下允许带刺，少客套，别拖长句。";
        }
        if (score >= 3) {
            return "当前情绪偏兴奋：更有网感和互动欲，允许接梗调侃，但别刷屏。";
        }
        if (score >= 1) {
            return "当前情绪较轻松：语气自然偏野，偶尔坏笑式调侃，保持短句。";
        }
        return "当前情绪平稳：像成熟狼系用户正常发言，直接、利落、别客服化。";
    }
}
