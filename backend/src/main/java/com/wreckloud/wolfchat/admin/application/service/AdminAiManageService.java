package com.wreckloud.wolfchat.admin.application.service;

import com.wreckloud.wolfchat.admin.api.dto.AdminAiRuntimeConfigDTO;
import com.wreckloud.wolfchat.admin.api.vo.AdminAiRuntimeConfigVO;
import com.wreckloud.wolfchat.ai.config.AiConfig;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

/**
 * 管理端 AI 管控服务（运行态配置）。
 */
@Service
@RequiredArgsConstructor
public class AdminAiManageService {
    private final AiConfig aiConfig;

    public AdminAiRuntimeConfigVO getRuntimeConfig() {
        AdminAiRuntimeConfigVO vo = new AdminAiRuntimeConfigVO();
        vo.setEnabled(aiConfig.getEnabled());
        vo.setProvider(aiConfig.getProvider());
        vo.setModel(aiConfig.getModel());
        vo.setApiKeyConfigured(StringUtils.hasText(aiConfig.getApiKey()));
        vo.setTemperature(aiConfig.getTemperature());
        vo.setMaxOutputTokens(aiConfig.getMaxOutputTokens());
        vo.setTimeoutMs(aiConfig.getTimeoutMs());

        AiConfig.Guard guard = aiConfig.getGuard();
        vo.setGuardEnabled(guard.getEnabled());
        vo.setGuardMaxCallsPerHour(guard.getMaxCallsPerHour());
        vo.setGuardMaxCallsPerDay(guard.getMaxCallsPerDay());

        AiConfig.Lobby lobby = aiConfig.getLobby();
        vo.setLobbyEnabled(lobby.getEnabled());
        vo.setLobbyBotUserId(lobby.getBotUserId());
        vo.setLobbyReplyProbability(lobby.getReplyProbability());
        vo.setLobbyMentionReplyProbability(lobby.getMentionReplyProbability());
        vo.setLobbyCooldownSeconds(lobby.getCooldownSeconds());
        vo.setLobbyMaxRepliesPerHour(lobby.getMaxRepliesPerHour());
        vo.setLobbySystemPrompt(lobby.getSystemPrompt());

        AiConfig.PrivateChat privateChat = aiConfig.getPrivateChat();
        vo.setPrivateChatEnabled(privateChat.getEnabled());
        vo.setPrivateChatBotUserId(privateChat.getBotUserId());
        vo.setPrivateChatReplyProbability(privateChat.getReplyProbability());
        vo.setPrivateChatCooldownSeconds(privateChat.getCooldownSeconds());
        vo.setPrivateChatMaxRepliesPerHour(privateChat.getMaxRepliesPerHour());
        vo.setPrivateChatSystemPrompt(privateChat.getSystemPrompt());

        AiConfig.Forum forum = aiConfig.getForum();
        vo.setForumEnabled(forum.getEnabled());
        vo.setForumBotUserId(forum.getBotUserId());
        vo.setForumReplyProbability(forum.getReplyProbability());
        vo.setForumMentionReplyProbability(forum.getMentionReplyProbability());
        vo.setForumReplyToReplyProbability(forum.getReplyToReplyProbability());
        vo.setForumCooldownSeconds(forum.getCooldownSeconds());
        vo.setForumMaxRepliesPerHour(forum.getMaxRepliesPerHour());
        vo.setForumMaxRepliesPerDay(forum.getMaxRepliesPerDay());
        vo.setForumSystemPrompt(forum.getSystemPrompt());

        AiConfig.Follow follow = aiConfig.getFollow();
        vo.setFollowAutoFollowBackEnabled(follow.getAutoFollowBackEnabled());
        vo.setFollowMinDelaySeconds(follow.getMinDelaySeconds());
        vo.setFollowMaxDelaySeconds(follow.getMaxDelaySeconds());
        return vo;
    }

    public synchronized AdminAiRuntimeConfigVO updateRuntimeConfig(AdminAiRuntimeConfigDTO dto) {
        if (dto == null) {
            return getRuntimeConfig();
        }
        aiConfig.setEnabled(pickBoolean(dto.getEnabled(), aiConfig.getEnabled()));
        aiConfig.setProvider(safeText(dto.getProvider(), aiConfig.getProvider(), 32));
        aiConfig.setModel(safeText(dto.getModel(), aiConfig.getModel(), 64));
        aiConfig.setTemperature(clampDouble(dto.getTemperature(), aiConfig.getTemperature(), 0.0D, 2.0D));
        aiConfig.setMaxOutputTokens(clampInt(dto.getMaxOutputTokens(), aiConfig.getMaxOutputTokens(), 64, 4096));
        aiConfig.setTimeoutMs(clampInt(dto.getTimeoutMs(), aiConfig.getTimeoutMs(), 3000, 120000));

        AiConfig.Guard guard = aiConfig.getGuard();
        guard.setEnabled(pickBoolean(dto.getGuardEnabled(), guard.getEnabled()));
        guard.setMaxCallsPerHour(clampInt(dto.getGuardMaxCallsPerHour(), guard.getMaxCallsPerHour(), 1, 10000));
        guard.setMaxCallsPerDay(clampInt(dto.getGuardMaxCallsPerDay(), guard.getMaxCallsPerDay(), 1, 200000));

        AiConfig.Lobby lobby = aiConfig.getLobby();
        lobby.setEnabled(pickBoolean(dto.getLobbyEnabled(), lobby.getEnabled()));
        lobby.setBotUserId(clampLong(dto.getLobbyBotUserId(), lobby.getBotUserId(), 1L, Long.MAX_VALUE));
        lobby.setReplyProbability(clampDouble(dto.getLobbyReplyProbability(), lobby.getReplyProbability(), 0.0D, 1.0D));
        lobby.setMentionReplyProbability(clampDouble(dto.getLobbyMentionReplyProbability(), lobby.getMentionReplyProbability(), 0.0D, 1.0D));
        lobby.setCooldownSeconds(clampInt(dto.getLobbyCooldownSeconds(), lobby.getCooldownSeconds(), 0, 3600));
        lobby.setMaxRepliesPerHour(clampInt(dto.getLobbyMaxRepliesPerHour(), lobby.getMaxRepliesPerHour(), 1, 1000));
        lobby.setSystemPrompt(safePrompt(dto.getLobbySystemPrompt(), lobby.getSystemPrompt()));

        AiConfig.PrivateChat privateChat = aiConfig.getPrivateChat();
        privateChat.setEnabled(pickBoolean(dto.getPrivateChatEnabled(), privateChat.getEnabled()));
        privateChat.setBotUserId(clampLong(dto.getPrivateChatBotUserId(), privateChat.getBotUserId(), 1L, Long.MAX_VALUE));
        privateChat.setReplyProbability(clampDouble(dto.getPrivateChatReplyProbability(), privateChat.getReplyProbability(), 0.0D, 1.0D));
        privateChat.setCooldownSeconds(clampInt(dto.getPrivateChatCooldownSeconds(), privateChat.getCooldownSeconds(), 0, 3600));
        privateChat.setMaxRepliesPerHour(clampInt(dto.getPrivateChatMaxRepliesPerHour(), privateChat.getMaxRepliesPerHour(), 1, 1000));
        privateChat.setSystemPrompt(safePrompt(dto.getPrivateChatSystemPrompt(), privateChat.getSystemPrompt()));

        AiConfig.Forum forum = aiConfig.getForum();
        forum.setEnabled(pickBoolean(dto.getForumEnabled(), forum.getEnabled()));
        forum.setBotUserId(clampLong(dto.getForumBotUserId(), forum.getBotUserId(), 1L, Long.MAX_VALUE));
        forum.setReplyProbability(clampDouble(dto.getForumReplyProbability(), forum.getReplyProbability(), 0.0D, 1.0D));
        forum.setMentionReplyProbability(clampDouble(dto.getForumMentionReplyProbability(), forum.getMentionReplyProbability(), 0.0D, 1.0D));
        forum.setReplyToReplyProbability(clampDouble(dto.getForumReplyToReplyProbability(), forum.getReplyToReplyProbability(), 0.0D, 1.0D));
        forum.setCooldownSeconds(clampInt(dto.getForumCooldownSeconds(), forum.getCooldownSeconds(), 0, 3600));
        forum.setMaxRepliesPerHour(clampInt(dto.getForumMaxRepliesPerHour(), forum.getMaxRepliesPerHour(), 1, 5000));
        forum.setMaxRepliesPerDay(clampInt(dto.getForumMaxRepliesPerDay(), forum.getMaxRepliesPerDay(), 1, 50000));
        forum.setSystemPrompt(safePrompt(dto.getForumSystemPrompt(), forum.getSystemPrompt()));

        AiConfig.Follow follow = aiConfig.getFollow();
        follow.setAutoFollowBackEnabled(pickBoolean(dto.getFollowAutoFollowBackEnabled(), follow.getAutoFollowBackEnabled()));
        int minDelay = clampInt(dto.getFollowMinDelaySeconds(), follow.getMinDelaySeconds(), 0, 3600);
        int maxDelay = clampInt(dto.getFollowMaxDelaySeconds(), follow.getMaxDelaySeconds(), 0, 3600);
        if (maxDelay < minDelay) {
            maxDelay = minDelay;
        }
        follow.setMinDelaySeconds(minDelay);
        follow.setMaxDelaySeconds(maxDelay);
        return getRuntimeConfig();
    }

    private Boolean pickBoolean(Boolean value, Boolean fallback) {
        return value != null ? value : fallback;
    }

    private Integer clampInt(Integer value, Integer fallback, int min, int max) {
        int source = value == null ? (fallback == null ? min : fallback) : value;
        return Math.max(min, Math.min(source, max));
    }

    private Long clampLong(Long value, Long fallback, long min, long max) {
        long source = value == null ? (fallback == null ? min : fallback) : value;
        return Math.max(min, Math.min(source, max));
    }

    private Double clampDouble(Double value, Double fallback, double min, double max) {
        double source = value == null ? (fallback == null ? min : fallback) : value;
        return Math.max(min, Math.min(source, max));
    }

    private String safeText(String value, String fallback, int maxLength) {
        if (!StringUtils.hasText(value)) {
            return fallback;
        }
        String trimmed = value.trim();
        if (trimmed.length() > maxLength) {
            return trimmed.substring(0, maxLength);
        }
        return trimmed;
    }

    private String safePrompt(String value, String fallback) {
        if (!StringUtils.hasText(value)) {
            return fallback;
        }
        String trimmed = value.trim();
        if (trimmed.length() > 2000) {
            return trimmed.substring(0, 2000);
        }
        return trimmed;
    }
}

