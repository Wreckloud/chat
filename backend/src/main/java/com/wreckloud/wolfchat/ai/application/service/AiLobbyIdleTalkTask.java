package com.wreckloud.wolfchat.ai.application.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.wreckloud.wolfchat.ai.application.client.AiTextClient;
import com.wreckloud.wolfchat.ai.config.AiConfig;
import com.wreckloud.wolfchat.chat.lobby.application.command.SendLobbyMessageCommand;
import com.wreckloud.wolfchat.chat.lobby.application.service.LobbyService;
import com.wreckloud.wolfchat.chat.lobby.domain.entity.WfLobbyMessage;
import com.wreckloud.wolfchat.chat.lobby.infra.mapper.WfLobbyMessageMapper;
import com.wreckloud.wolfchat.chat.message.application.service.ChatMessagePushService;
import com.wreckloud.wolfchat.chat.message.domain.enums.MessageType;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

import java.time.LocalDateTime;
import java.util.Collections;
import java.util.List;

/**
 * 大厅冷场主动发言任务。
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class AiLobbyIdleTalkTask {
    private static final String LOBBY_SCENE = "lobby";
    private static final String LOBBY_IDLE_SCENE = "lobby-idle";
    private static final String DEDUP_KEY_PREFIX = "lobby:idle:";
    private static final int DEFAULT_IDLE_TRIGGER_SECONDS = 90;
    private static final double DEFAULT_IDLE_SPEAK_PROBABILITY = 0.08D;
    private static final int DEFAULT_MAX_REPLY_CHARS = 140;
    private static final int DEFAULT_CONTEXT_LIMIT = 20;

    private final AiConfig aiConfig;
    private final AiTextClient aiTextClient;
    private final AiIdentityService aiIdentityService;
    private final AiRoleService aiRoleService;
    private final AiMoodService aiMoodService;
    private final AiMemoryDigestService aiMemoryDigestService;
    private final AiPromptBuilderService aiPromptBuilderService;
    private final AiRateLimitService aiRateLimitService;
    private final AiTaskSchedulerService aiTaskSchedulerService;
    private final LobbyService lobbyService;
    private final ChatMessagePushService chatMessagePushService;
    private final WfLobbyMessageMapper wfLobbyMessageMapper;

    @Scheduled(fixedDelay = 30000, initialDelay = 20000)
    public void tryIdleSpeak() {
        try {
            if (!isRuntimeReady()) {
                return;
            }
            AiConfig.Lobby lobbyConfig = aiConfig.getLobby();
            if (lobbyConfig == null
                    || !Boolean.TRUE.equals(lobbyConfig.getEnabled())
                    || !Boolean.TRUE.equals(lobbyConfig.getIdleEnabled())) {
                return;
            }
            Long botUserId = aiIdentityService.getLobbyBotUserId();
            if (botUserId == null || botUserId <= 0L) {
                return;
            }
            if (!isLobbyIdle(resolveIdleTriggerSeconds(lobbyConfig.getIdleTriggerSeconds()))) {
                return;
            }
            if (!hitProbability(lobbyConfig.getIdleSpeakProbability(), DEFAULT_IDLE_SPEAK_PROBABILITY)) {
                return;
            }
            if (!aiRateLimitService.allowByCooldown(LOBBY_IDLE_SCENE, botUserId, "global", lobbyConfig.getCooldownSeconds())) {
                return;
            }
            if (!aiRateLimitService.allowByHourlyLimit(LOBBY_SCENE, botUserId, lobbyConfig.getMaxRepliesPerHour())) {
                return;
            }
            if (!allowGlobalQuota(botUserId)) {
                return;
            }
            String dedupKey = DEDUP_KEY_PREFIX + botUserId;
            if (aiTaskSchedulerService.isPending(dedupKey)) {
                return;
            }
            aiTaskSchedulerService.schedule(
                    dedupKey,
                    lobbyConfig.getMinDelaySeconds(),
                    lobbyConfig.getMaxDelaySeconds(),
                    () -> processIdleSpeak(botUserId, lobbyConfig)
            );
        } catch (Exception ex) {
            log.warn("AI 大厅冷场任务异常: {}", ex.getMessage());
        }
    }

    private void processIdleSpeak(Long botUserId, AiConfig.Lobby lobbyConfig) {
        try {
            List<WfLobbyMessage> recentMessages = loadRecentLobbyMessages(resolveContextLimit());
            AiRoleService.AiRoleProfile roleProfile = aiRoleService.pickRole(LOBBY_SCENE);
            String rolePrompt = buildRolePrompt(roleProfile);
            String moodDirective = aiMoodService.buildMoodDirective(LOBBY_SCENE, botUserId, "冷场开话题");
            String memoryDigest = aiMemoryDigestService.buildLobbyDigest(recentMessages);
            String prompt = aiPromptBuilderService.buildLobbyPrompt(
                    botUserId, botUserId, recentMessages, rolePrompt, moodDirective, memoryDigest
            );
            prompt = prompt + "\n当前任务：聊天室冷场，请主动发一条自然的开场话题，控制在一句话内。";
            String systemPrompt = normalizeSystemPrompt(lobbyConfig.getSystemPrompt());
            systemPrompt = appendRoleSystemPrompt(systemPrompt, roleProfile);
            String reply = normalizeReply(
                    aiTextClient.complete(systemPrompt, prompt),
                    lobbyConfig.getMaxReplyChars()
            );
            if (!StringUtils.hasText(reply)) {
                return;
            }
            SendLobbyMessageCommand command = new SendLobbyMessageCommand();
            command.setUserId(botUserId);
            command.setMsgType(MessageType.TEXT);
            command.setContent(reply);
            chatMessagePushService.pushLobbyMessage(botUserId, lobbyService.sendMessage(command));
            log.info("AI 大厅冷场发言完成: botUserId={}, role={}", botUserId, roleCode(roleProfile));
        } catch (Exception ex) {
            log.warn("AI 大厅冷场发言失败: botUserId={}, message={}", botUserId, ex.getMessage());
        }
    }

    private boolean isRuntimeReady() {
        return aiIdentityService.isAiEnabled() && aiTextClient.isAvailable();
    }

    private int resolveIdleTriggerSeconds(Integer configured) {
        if (configured == null || configured <= 0) {
            return DEFAULT_IDLE_TRIGGER_SECONDS;
        }
        return Math.max(30, Math.min(configured, 600));
    }

    private int resolveContextLimit() {
        Integer configured = aiConfig.getMemory() == null ? null : aiConfig.getMemory().getRecentWindowSize();
        if (configured == null || configured <= 0) {
            return DEFAULT_CONTEXT_LIMIT;
        }
        return Math.max(8, Math.min(configured, 30));
    }

    private boolean isLobbyIdle(int idleTriggerSeconds) {
        LocalDateTime latestMessageTime = wfLobbyMessageMapper.selectLatestMessageTime();
        if (latestMessageTime == null) {
            return true;
        }
        return latestMessageTime.isBefore(LocalDateTime.now().minusSeconds(idleTriggerSeconds));
    }

    private boolean allowGlobalQuota(Long botUserId) {
        AiConfig.Guard guard = aiConfig.getGuard();
        if (guard == null || !Boolean.TRUE.equals(guard.getEnabled())) {
            return true;
        }
        if (!aiRateLimitService.allowByGlobalHourlyLimit(botUserId, guard.getMaxCallsPerHour())) {
            return false;
        }
        return aiRateLimitService.allowByGlobalDailyLimit(botUserId, guard.getMaxCallsPerDay());
    }

    private boolean hitProbability(Double configuredProbability, double defaultProbability) {
        double probability = configuredProbability == null ? defaultProbability : configuredProbability;
        if (probability <= 0D) {
            return false;
        }
        if (probability >= 1D) {
            return true;
        }
        return Math.random() < probability;
    }

    private List<WfLobbyMessage> loadRecentLobbyMessages(int contextLimit) {
        if (contextLimit <= 0) {
            return Collections.emptyList();
        }
        LambdaQueryWrapper<WfLobbyMessage> queryWrapper = new LambdaQueryWrapper<>();
        queryWrapper.notIn(WfLobbyMessage::getMsgType, List.of(MessageType.RECALL))
                .orderByDesc(WfLobbyMessage::getCreateTime)
                .orderByDesc(WfLobbyMessage::getId)
                .last("LIMIT " + contextLimit);
        List<WfLobbyMessage> rows = wfLobbyMessageMapper.selectList(queryWrapper);
        if (rows.isEmpty()) {
            return rows;
        }
        Collections.reverse(rows);
        return rows;
    }

    private String normalizeSystemPrompt(String configuredPrompt) {
        if (StringUtils.hasText(configuredPrompt)) {
            return configuredPrompt.trim();
        }
        return "你在公共聊天室聊天，请像普通网友一样自然参与，不要刷屏。";
    }

    private String appendRoleSystemPrompt(String systemPrompt, AiRoleService.AiRoleProfile roleProfile) {
        if (roleProfile == null) {
            return systemPrompt;
        }
        StringBuilder builder = new StringBuilder(systemPrompt);
        if (StringUtils.hasText(roleProfile.getPersonaPrompt())) {
            builder.append("\n角色人设补充：").append(roleProfile.getPersonaPrompt().trim());
        }
        if (StringUtils.hasText(roleProfile.getStylePrompt())) {
            builder.append("\n表达风格补充：").append(roleProfile.getStylePrompt().trim());
        }
        return builder.toString();
    }

    private String buildRolePrompt(AiRoleService.AiRoleProfile roleProfile) {
        if (roleProfile == null) {
            return null;
        }
        StringBuilder builder = new StringBuilder();
        if (StringUtils.hasText(roleProfile.getRoleName())) {
            builder.append("当前角色：").append(roleProfile.getRoleName().trim()).append("。");
        }
        if (StringUtils.hasText(roleProfile.getPersonaPrompt())) {
            builder.append(roleProfile.getPersonaPrompt().trim()).append('。');
        }
        if (StringUtils.hasText(roleProfile.getStylePrompt())) {
            builder.append(roleProfile.getStylePrompt().trim());
        }
        String text = builder.toString().trim();
        return StringUtils.hasText(text) ? text : null;
    }

    private String normalizeReply(String raw, Integer configuredMaxChars) {
        if (!StringUtils.hasText(raw)) {
            return null;
        }
        String normalized = raw.replace("\r", "").trim();
        if (!StringUtils.hasText(normalized)) {
            return null;
        }
        int maxChars = resolveMaxReplyChars(configuredMaxChars);
        if (normalized.length() <= maxChars) {
            return normalized;
        }
        return normalized.substring(0, maxChars);
    }

    private int resolveMaxReplyChars(Integer configuredMaxChars) {
        if (configuredMaxChars == null || configuredMaxChars <= 0) {
            return DEFAULT_MAX_REPLY_CHARS;
        }
        return Math.max(60, Math.min(configuredMaxChars, 300));
    }

    private String roleCode(AiRoleService.AiRoleProfile roleProfile) {
        if (roleProfile == null || !StringUtils.hasText(roleProfile.getRoleCode())) {
            return "default";
        }
        return roleProfile.getRoleCode();
    }
}

