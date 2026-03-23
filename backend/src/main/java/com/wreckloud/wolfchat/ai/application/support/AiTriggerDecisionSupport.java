package com.wreckloud.wolfchat.ai.application.support;

import com.wreckloud.wolfchat.ai.application.service.AiIdentityService;
import com.wreckloud.wolfchat.chat.lobby.application.event.LobbyMessageSentEvent;
import com.wreckloud.wolfchat.chat.lobby.domain.entity.WfLobbyMessage;
import com.wreckloud.wolfchat.community.application.event.ForumReplyCreatedEvent;
import com.wreckloud.wolfchat.community.domain.entity.WfForumReply;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.util.function.Function;

/**
 * @Description AI 触发判定支持
 * @Author Wreckloud
 * @Date 2026-03-23
 */
@Component
@RequiredArgsConstructor
public class AiTriggerDecisionSupport {
    private final AiIdentityService aiIdentityService;
    private final AiLobbyDialogueIntentSupport aiLobbyDialogueIntentSupport;

    public boolean isDirectedLobbyMessage(LobbyMessageSentEvent event,
                                          WfLobbyMessage triggerMessage,
                                          Long botUserId,
                                          Function<Long, String> displayNameResolver) {
        if (event == null || !isValidUserId(botUserId)) {
            return false;
        }
        if (triggerMessage != null && botUserId.equals(triggerMessage.getReplyToSenderId())) {
            return true;
        }
        if (containsAiMention(botUserId, event.getContent())) {
            return true;
        }
        if (!aiLobbyDialogueIntentSupport.looksLikeLobbyFollowUpIntent(event.getContent())) {
            return false;
        }
        return aiLobbyDialogueIntentSupport.hasImplicitLobbyContext(
                event.getSenderId(),
                botUserId,
                triggerMessage == null ? null : triggerMessage.getId(),
                displayNameResolver
        );
    }

    public boolean isDirectedForumReply(ForumReplyCreatedEvent event, WfForumReply quoteReply, Long botUserId) {
        if (event == null || !isValidUserId(botUserId)) {
            return false;
        }
        if (quoteReply != null && botUserId.equals(quoteReply.getAuthorId())) {
            return true;
        }
        return containsAiMention(botUserId, event.getContent());
    }

    public boolean containsAiMention(Long aiUserId, String... texts) {
        if (!isValidUserId(aiUserId) || texts == null || texts.length == 0) {
            return false;
        }
        for (String text : texts) {
            if (aiIdentityService.isMentionToAi(aiUserId, text)) {
                return true;
            }
        }
        return false;
    }

    private boolean isValidUserId(Long userId) {
        return userId != null && userId > 0L;
    }
}

