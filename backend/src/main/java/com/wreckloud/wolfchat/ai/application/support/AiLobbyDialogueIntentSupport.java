package com.wreckloud.wolfchat.ai.application.support;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.wreckloud.wolfchat.ai.application.service.AiIdentityService;
import com.wreckloud.wolfchat.ai.config.AiConfig;
import com.wreckloud.wolfchat.chat.lobby.domain.entity.WfLobbyMessage;
import com.wreckloud.wolfchat.chat.lobby.infra.mapper.WfLobbyMessageMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

import java.time.Duration;
import java.time.LocalDateTime;
import java.util.List;
import java.util.function.Function;

/**
 * @Description Lobby 场景对话意图与隐式上下文判断
 * @Author Wreckloud
 * @Date 2026-03-23
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class AiLobbyDialogueIntentSupport {
    private static final int LOBBY_IMPLICIT_DIALOGUE_WINDOW_SECONDS = 180;
    private static final int LOBBY_IMPLICIT_CONTEXT_LIMIT = 20;
    private static final int LOBBY_MIN_INTENT_CHARS = 2;
    private static final int LOBBY_FOLLOW_UP_MIN_CHARS = 4;
    private static final int DEFAULT_ADJACENT_WINDOW_SECONDS = 75;
    private static final int DEFAULT_ADJACENT_MAX_INTERVENING_MESSAGES = 2;
    private static final int ADJACENT_CONTEXT_LIMIT = 16;
    private static final int RHYTHM_SAMPLE_MIN_MESSAGES = 4;
    private static final int FAST_RHYTHM_GAP_SECONDS = 12;
    private static final int SLOW_RHYTHM_GAP_SECONDS = 45;
    private static final List<String> LOBBY_INTENT_KEYWORDS = List.of(
            "你", "你是", "你在", "你会", "你能", "你怎么", "你觉得",
            "聊", "说说", "解释", "啥意思", "什么意思", "为啥", "怎么", "为什么",
            "可以吗", "行吗", "能不能", "要不要", "是不是", "对吗", "吗", "？", "?",
            "回复", "回我", "别走", "继续", "在吗", "在线吗",
            "你好", "哈喽", "嗨", "在不在", "有人吗", "还在吗"
    );
    private static final List<String> LOW_SIGNAL_EXACT_TEXTS = List.of(
            "嗯", "嗯嗯", "哦", "哦哦", "啊", "额", "emm", "emmm", "。。。", "...", "6", "66", "666", "?"
    );

    private final AiConfig aiConfig;
    private final AiIdentityService aiIdentityService;
    private final WfLobbyMessageMapper wfLobbyMessageMapper;

    public boolean looksLikeLobbyFollowUpIntent(String content) {
        if (!StringUtils.hasText(content)) {
            return false;
        }
        String normalized = content.trim();
        if (!StringUtils.hasText(normalized) || normalized.length() < LOBBY_MIN_INTENT_CHARS) {
            return false;
        }
        if (normalized.startsWith("@")) {
            return true;
        }
        String lowerCaseText = normalized.toLowerCase();
        for (String keyword : LOBBY_INTENT_KEYWORDS) {
            if (lowerCaseText.contains(keyword.toLowerCase())) {
                return true;
            }
        }
        return normalized.length() >= LOBBY_FOLLOW_UP_MIN_CHARS;
    }

    public boolean hasImplicitLobbyContext(Long senderId,
                                           Long botUserId,
                                           Long triggerMessageId,
                                           Function<Long, String> displayNameResolver) {
        if (!isValidUserId(senderId) || !isValidUserId(botUserId)) {
            return false;
        }
        LocalDateTime threshold = LocalDateTime.now().minusSeconds(LOBBY_IMPLICIT_DIALOGUE_WINDOW_SECONDS);
        LambdaQueryWrapper<WfLobbyMessage> queryWrapper = new LambdaQueryWrapper<>();
        queryWrapper.ge(WfLobbyMessage::getCreateTime, threshold)
                .orderByDesc(WfLobbyMessage::getCreateTime)
                .orderByDesc(WfLobbyMessage::getId)
                .last("LIMIT " + LOBBY_IMPLICIT_CONTEXT_LIMIT);
        List<WfLobbyMessage> recentMessages = wfLobbyMessageMapper.selectList(queryWrapper);
        if (recentMessages == null || recentMessages.isEmpty()) {
            return false;
        }

        int triggerIndex = findTriggerMessageIndex(recentMessages, triggerMessageId);
        boolean reachedTrigger = triggerIndex < 0;
        for (int i = 0; i < recentMessages.size(); i++) {
            WfLobbyMessage current = recentMessages.get(i);
            if (current == null) {
                continue;
            }
            if (!reachedTrigger) {
                if (i == triggerIndex) {
                    reachedTrigger = true;
                }
                continue;
            }
            if (botUserId.equals(current.getSenderId())) {
                if (isBotMessageDirectedToSender(current, senderId, displayNameResolver)) {
                    return true;
                }
                continue;
            }
            if (!senderId.equals(current.getSenderId())) {
                return false;
            }
        }
        return false;
    }

    public boolean hasAdjacentBotReplyContext(Long senderId, Long botUserId, WfLobbyMessage triggerMessage) {
        if (!isValidUserId(senderId) || !isValidUserId(botUserId) || triggerMessage == null || triggerMessage.getId() == null) {
            return false;
        }
        AiConfig.Lobby lobbyConfig = aiConfig.getLobby();
        if (lobbyConfig != null && Boolean.FALSE.equals(lobbyConfig.getAdjacentDirectedEnabled())) {
            return false;
        }
        if (botUserId.equals(triggerMessage.getSenderId())) {
            return false;
        }
        if (isDirectedToOtherUser(triggerMessage, botUserId)) {
            return false;
        }
        int baseWindowSeconds = resolveAdjacentWindowSeconds(lobbyConfig);
        LocalDateTime threshold = LocalDateTime.now().minusSeconds(Math.max(baseWindowSeconds, 120));
        LambdaQueryWrapper<WfLobbyMessage> queryWrapper = new LambdaQueryWrapper<>();
        queryWrapper.ge(WfLobbyMessage::getCreateTime, threshold)
                .orderByDesc(WfLobbyMessage::getCreateTime)
                .orderByDesc(WfLobbyMessage::getId)
                .last("LIMIT " + ADJACENT_CONTEXT_LIMIT);
        List<WfLobbyMessage> recentMessages = wfLobbyMessageMapper.selectList(queryWrapper);
        if (recentMessages == null || recentMessages.isEmpty()) {
            return false;
        }
        int triggerIndex = findTriggerMessageIndex(recentMessages, triggerMessage.getId());
        if (triggerIndex < 0) {
            return false;
        }
        AdjacentWindowPolicy policy = resolveAdjacentPolicy(lobbyConfig, recentMessages);
        int nonBotBetween = 0;
        for (int i = triggerIndex + 1; i < recentMessages.size(); i++) {
            WfLobbyMessage olderMessage = recentMessages.get(i);
            if (olderMessage == null) {
                continue;
            }
            if (triggerMessage.getCreateTime() != null && olderMessage.getCreateTime() != null) {
                long ageSeconds = Math.abs(Duration.between(olderMessage.getCreateTime(), triggerMessage.getCreateTime()).toSeconds());
                if (ageSeconds > policy.windowSeconds()) {
                    return false;
                }
            }
            if (botUserId.equals(olderMessage.getSenderId())) {
                boolean matched = nonBotBetween <= policy.maxInterveningMessages();
                if (matched) {
                    log.debug("AI 紧邻语境命中: senderId={}, window={}s, maxIntervening={}, rhythm={}",
                            senderId, policy.windowSeconds(), policy.maxInterveningMessages(), policy.rhythmLabel());
                }
                return matched;
            }
            if (senderId.equals(olderMessage.getSenderId())) {
                continue;
            }
            nonBotBetween++;
            if (nonBotBetween > policy.maxInterveningMessages()) {
                return false;
            }
        }
        return false;
    }

    private int findTriggerMessageIndex(List<WfLobbyMessage> recentMessages, Long triggerMessageId) {
        if (recentMessages == null || recentMessages.isEmpty() || triggerMessageId == null || triggerMessageId <= 0L) {
            return -1;
        }
        for (int i = 0; i < recentMessages.size(); i++) {
            WfLobbyMessage message = recentMessages.get(i);
            if (message != null && triggerMessageId.equals(message.getId())) {
                return i;
            }
        }
        return -1;
    }

    private boolean isBotMessageDirectedToSender(WfLobbyMessage botMessage,
                                                 Long senderId,
                                                 Function<Long, String> displayNameResolver) {
        if (botMessage == null || !isValidUserId(senderId)) {
            return false;
        }
        if (senderId.equals(botMessage.getReplyToSenderId())) {
            return true;
        }
        if (displayNameResolver == null) {
            return false;
        }
        String senderName = displayNameResolver.apply(senderId);
        if (!StringUtils.hasText(senderName) || !StringUtils.hasText(botMessage.getContent())) {
            return false;
        }
        String normalizedContent = botMessage.getContent().trim();
        if (!StringUtils.hasText(normalizedContent)) {
            return false;
        }
        return normalizedContent.startsWith("@" + senderName.trim());
    }

    private boolean isValidUserId(Long userId) {
        return userId != null && userId > 0L;
    }

    private boolean isDirectedToOtherUser(WfLobbyMessage triggerMessage, Long botUserId) {
        if (triggerMessage == null || !isValidUserId(botUserId)) {
            return false;
        }
        if (isValidUserId(triggerMessage.getReplyToSenderId()) && !botUserId.equals(triggerMessage.getReplyToSenderId())) {
            return true;
        }
        if (!StringUtils.hasText(triggerMessage.getContent())) {
            return false;
        }
        String text = triggerMessage.getContent().trim();
        if (!text.startsWith("@")) {
            return false;
        }
        return !aiIdentityService.isMentionToAi(botUserId, text);
    }

    public boolean isLowSignalLobbyText(String content) {
        if (!StringUtils.hasText(content)) {
            return true;
        }
        String normalized = content.replace('\n', ' ').replace('\r', ' ').trim();
        if (!StringUtils.hasText(normalized)) {
            return true;
        }
        if (normalized.startsWith("@")) {
            return false;
        }
        String lower = normalized.toLowerCase();
        if (LOW_SIGNAL_EXACT_TEXTS.contains(lower)) {
            return true;
        }
        if (containsAnyIntentKeyword(lower)) {
            return false;
        }
        String compact = lower.replace(" ", "");
        if (compact.length() <= 2) {
            return true;
        }
        return compact.length() <= 4 && onlyPunctuationOrRepeatedChars(compact);
    }

    private boolean containsAnyIntentKeyword(String lowerCaseText) {
        if (!StringUtils.hasText(lowerCaseText)) {
            return false;
        }
        for (String keyword : LOBBY_INTENT_KEYWORDS) {
            if (lowerCaseText.contains(keyword.toLowerCase())) {
                return true;
            }
        }
        return false;
    }

    private boolean onlyPunctuationOrRepeatedChars(String text) {
        if (!StringUtils.hasText(text)) {
            return true;
        }
        boolean allPunctuation = true;
        for (int i = 0; i < text.length(); i++) {
            char ch = text.charAt(i);
            if (Character.isLetterOrDigit(ch) || Character.isIdeographic(ch)) {
                allPunctuation = false;
                break;
            }
        }
        if (allPunctuation) {
            return true;
        }
        char first = text.charAt(0);
        for (int i = 1; i < text.length(); i++) {
            if (text.charAt(i) != first) {
                return false;
            }
        }
        return true;
    }

    private AdjacentWindowPolicy resolveAdjacentPolicy(AiConfig.Lobby lobbyConfig, List<WfLobbyMessage> recentMessages) {
        int baseWindow = resolveAdjacentWindowSeconds(lobbyConfig);
        int baseMaxIntervening = resolveAdjacentMaxInterveningMessages(lobbyConfig);
        RhythmLevel rhythmLevel = estimateLobbyRhythm(recentMessages);
        if (RhythmLevel.FAST.equals(rhythmLevel)) {
            return new AdjacentWindowPolicy(
                    Math.max(45, Math.min(baseWindow, 60)),
                    Math.min(baseMaxIntervening + 1, 5),
                    "fast"
            );
        }
        if (RhythmLevel.SLOW.equals(rhythmLevel)) {
            return new AdjacentWindowPolicy(
                    Math.min(120, Math.max(baseWindow, 90)),
                    Math.max(1, baseMaxIntervening - 1),
                    "slow"
            );
        }
        return new AdjacentWindowPolicy(baseWindow, baseMaxIntervening, "normal");
    }

    private RhythmLevel estimateLobbyRhythm(List<WfLobbyMessage> recentMessages) {
        if (recentMessages == null || recentMessages.size() < RHYTHM_SAMPLE_MIN_MESSAGES) {
            return RhythmLevel.NORMAL;
        }
        long totalGapSeconds = 0L;
        int sampleCount = 0;
        for (int i = 0; i + 1 < recentMessages.size() && sampleCount < 8; i++) {
            WfLobbyMessage newer = recentMessages.get(i);
            WfLobbyMessage older = recentMessages.get(i + 1);
            if (newer == null || older == null || newer.getCreateTime() == null || older.getCreateTime() == null) {
                continue;
            }
            long gap = Math.abs(Duration.between(older.getCreateTime(), newer.getCreateTime()).toSeconds());
            if (gap <= 0 || gap > 180) {
                continue;
            }
            totalGapSeconds += gap;
            sampleCount++;
        }
        if (sampleCount < 3) {
            return RhythmLevel.NORMAL;
        }
        long averageGapSeconds = totalGapSeconds / sampleCount;
        if (averageGapSeconds <= FAST_RHYTHM_GAP_SECONDS) {
            return RhythmLevel.FAST;
        }
        if (averageGapSeconds >= SLOW_RHYTHM_GAP_SECONDS) {
            return RhythmLevel.SLOW;
        }
        return RhythmLevel.NORMAL;
    }

    private int resolveAdjacentWindowSeconds(AiConfig.Lobby lobbyConfig) {
        Integer configured = lobbyConfig == null ? null : lobbyConfig.getAdjacentDirectedWindowSeconds();
        if (configured == null || configured <= 0) {
            return DEFAULT_ADJACENT_WINDOW_SECONDS;
        }
        return Math.max(20, Math.min(configured, 180));
    }

    private int resolveAdjacentMaxInterveningMessages(AiConfig.Lobby lobbyConfig) {
        Integer configured = lobbyConfig == null ? null : lobbyConfig.getAdjacentDirectedMaxInterveningMessages();
        if (configured == null || configured < 0) {
            return DEFAULT_ADJACENT_MAX_INTERVENING_MESSAGES;
        }
        return Math.min(configured, 5);
    }

    private record AdjacentWindowPolicy(int windowSeconds, int maxInterveningMessages, String rhythmLabel) {
    }

    private enum RhythmLevel {
        FAST,
        NORMAL,
        SLOW
    }
}

