package com.wreckloud.wolfchat.ai.application.support;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.wreckloud.wolfchat.chat.lobby.domain.entity.WfLobbyMessage;
import com.wreckloud.wolfchat.chat.lobby.infra.mapper.WfLobbyMessageMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

import java.time.LocalDateTime;
import java.util.List;
import java.util.function.Function;

/**
 * @Description Lobby 场景对话意图与隐式上下文判断
 * @Author Wreckloud
 * @Date 2026-03-23
 */
@Component
@RequiredArgsConstructor
public class AiLobbyDialogueIntentSupport {
    private static final int LOBBY_IMPLICIT_DIALOGUE_WINDOW_SECONDS = 180;
    private static final int LOBBY_IMPLICIT_CONTEXT_LIMIT = 20;
    private static final int LOBBY_MIN_INTENT_CHARS = 2;
    private static final int LOBBY_FOLLOW_UP_MIN_CHARS = 4;
    private static final List<String> LOBBY_INTENT_KEYWORDS = List.of(
            "你", "你是", "你在", "你会", "你能", "你怎么", "你觉得",
            "聊", "说说", "解释", "啥意思", "什么意思", "为啥", "怎么", "为什么",
            "可以吗", "行吗", "能不能", "要不要", "是不是", "对吗", "吗", "？", "?",
            "回复", "回我", "别走", "继续", "在吗", "在线吗"
    );

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
}

