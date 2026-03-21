package com.wreckloud.wolfchat.ai.application.service;

import com.wreckloud.wolfchat.account.application.service.UserService;
import com.wreckloud.wolfchat.account.domain.entity.WfUser;
import com.wreckloud.wolfchat.ai.config.AiConfig;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import java.util.Collections;
import java.util.LinkedHashSet;
import java.util.Map;
import java.util.Set;

/**
 * AI 账号身份解析服务。
 */
@Service
@RequiredArgsConstructor
public class AiIdentityService {
    private final AiConfig aiConfig;
    private final UserService userService;

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

    public boolean isMentionToAi(Long aiUserId, String content) {
        if (!isPositive(aiUserId) || !StringUtils.hasText(content)) {
            return false;
        }
        String text = content.trim();
        if (!StringUtils.hasText(text)) {
            return false;
        }
        for (String keyword : listAiMentionKeywords(aiUserId)) {
            if (!StringUtils.hasText(keyword)) {
                continue;
            }
            if (text.contains("@" + keyword.trim())) {
                return true;
            }
        }
        return false;
    }

    public String resolveAiDisplayName(Long aiUserId) {
        if (!isPositive(aiUserId)) {
            return null;
        }
        WfUser aiUser = loadAiUser(aiUserId);
        if (aiUser != null && StringUtils.hasText(aiUser.getNickname())) {
            return aiUser.getNickname().trim();
        }
        if (aiUser != null && StringUtils.hasText(aiUser.getWolfNo())) {
            return aiUser.getWolfNo().trim();
        }
        if (getLobbyBotUserId() != null
                && getLobbyBotUserId().equals(aiUserId)
                && aiConfig.getLobby() != null
                && StringUtils.hasText(aiConfig.getLobby().getBotDisplayName())) {
            return aiConfig.getLobby().getBotDisplayName().trim();
        }
        return "user#" + aiUserId;
    }

    private Set<String> listAiMentionKeywords(Long aiUserId) {
        Set<String> keywords = new LinkedHashSet<>();
        if (getLobbyBotUserId() != null
                && getLobbyBotUserId().equals(aiUserId)
                && aiConfig.getLobby() != null
                && StringUtils.hasText(aiConfig.getLobby().getBotDisplayName())) {
            keywords.add(aiConfig.getLobby().getBotDisplayName().trim());
        }
        WfUser aiUser = loadAiUser(aiUserId);
        if (aiUser != null && StringUtils.hasText(aiUser.getNickname())) {
            keywords.add(aiUser.getNickname().trim());
        }
        if (aiUser != null && StringUtils.hasText(aiUser.getWolfNo())) {
            keywords.add(aiUser.getWolfNo().trim());
        }
        return keywords;
    }

    private WfUser loadAiUser(Long aiUserId) {
        try {
            Map<Long, WfUser> userMap = userService.getUserMap(Collections.singleton(aiUserId));
            return userMap.get(aiUserId);
        } catch (Exception ignored) {
            return null;
        }
    }

    private void addIfValid(Set<Long> holder, Long userId) {
        if (userId == null || userId <= 0L) {
            return;
        }
        holder.add(userId);
    }

    private boolean isPositive(Long value) {
        return value != null && value > 0L;
    }
}
