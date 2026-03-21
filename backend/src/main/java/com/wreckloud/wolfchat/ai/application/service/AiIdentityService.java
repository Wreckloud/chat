package com.wreckloud.wolfchat.ai.application.service;

import com.wreckloud.wolfchat.ai.config.AiConfig;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import java.util.LinkedHashSet;
import java.util.Set;

/**
 * AI 账号身份解析服务。
 */
@Service
public class AiIdentityService {
    private final AiConfig aiConfig;

    public AiIdentityService(AiConfig aiConfig) {
        this.aiConfig = aiConfig;
    }

    public boolean isAiEnabled() {
        return Boolean.TRUE.equals(aiConfig.getEnabled())
                && StringUtils.hasText(aiConfig.getModel())
                && StringUtils.hasText(aiConfig.getApiKey());
    }

    public Long getLobbyBotUserId() {
        if (aiConfig.getLobby() == null) {
            return null;
        }
        return aiConfig.getLobby().getBotUserId();
    }

    public Long getPrivateBotUserId() {
        Long configured = aiConfig.getPrivateChat() == null
                ? null
                : aiConfig.getPrivateChat().getBotUserId();
        return configured != null ? configured : getLobbyBotUserId();
    }

    public Long getForumBotUserId() {
        Long configured = aiConfig.getForum() == null
                ? null
                : aiConfig.getForum().getBotUserId();
        return configured != null ? configured : getLobbyBotUserId();
    }

    public boolean isAiUser(Long userId) {
        if (userId == null || userId <= 0L) {
            return false;
        }
        for (Long aiUserId : listAiUserIds()) {
            if (userId.equals(aiUserId)) {
                return true;
            }
        }
        return false;
    }

    public Set<Long> listAiUserIds() {
        Set<Long> aiUserIds = new LinkedHashSet<>();
        addIfValid(aiUserIds, getLobbyBotUserId());
        addIfValid(aiUserIds, getPrivateBotUserId());
        addIfValid(aiUserIds, getForumBotUserId());
        return aiUserIds;
    }

    private void addIfValid(Set<Long> holder, Long userId) {
        if (userId == null || userId <= 0L) {
            return;
        }
        holder.add(userId);
    }
}

