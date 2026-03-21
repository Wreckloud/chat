package com.wreckloud.wolfchat.ai.application.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.wreckloud.wolfchat.ai.application.client.AiTextClient;
import com.wreckloud.wolfchat.ai.config.AiConfig;
import com.wreckloud.wolfchat.chat.presence.application.service.UserPresenceService;
import com.wreckloud.wolfchat.community.api.dto.CreateThreadDTO;
import com.wreckloud.wolfchat.community.application.service.ForumService;
import com.wreckloud.wolfchat.community.domain.entity.WfForumThread;
import com.wreckloud.wolfchat.community.domain.enums.ForumThreadStatus;
import com.wreckloud.wolfchat.community.infra.mapper.WfForumThreadMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

import java.time.LocalDateTime;
import java.util.Collections;
import java.util.List;

/**
 * 论坛冷场主动发主题任务（低频、限额）。
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class AiForumIdlePostTask {
    private static final String FORUM_SCENE = "forum";
    private static final String FORUM_POST_SCENE = "forum-post";
    private static final String DEDUP_KEY_PREFIX = "forum:idle:post:";
    private static final int DEFAULT_IDLE_TRIGGER_SECONDS = 1800;
    private static final double DEFAULT_IDLE_POST_PROBABILITY = 0.05D;
    private static final int DEFAULT_CONTEXT_LIMIT = 20;
    private static final int DEFAULT_MAX_TITLE_CHARS = 36;
    private static final int DEFAULT_MAX_CONTENT_CHARS = 260;
    private static final int DEFAULT_MAX_POSTS_PER_HOUR = 2;
    private static final int DEFAULT_MAX_POSTS_PER_DAY = 8;

    private final AiConfig aiConfig;
    private final AiTextClient aiTextClient;
    private final AiIdentityService aiIdentityService;
    private final AiRoleService aiRoleService;
    private final AiMoodService aiMoodService;
    private final AiMemoryDigestService aiMemoryDigestService;
    private final AiInteractionMemoryService aiInteractionMemoryService;
    private final AiPromptBuilderService aiPromptBuilderService;
    private final AiRateLimitService aiRateLimitService;
    private final AiTaskSchedulerService aiTaskSchedulerService;
    private final ForumService forumService;
    private final UserPresenceService userPresenceService;
    private final WfForumThreadMapper wfForumThreadMapper;

    @Scheduled(fixedDelay = 60000, initialDelay = 40000)
    public void tryIdlePost() {
        try {
            if (!isRuntimeReady()) {
                return;
            }
            AiConfig.Forum forumConfig = aiConfig.getForum();
            if (forumConfig == null
                    || !Boolean.TRUE.equals(forumConfig.getEnabled())
                    || !Boolean.TRUE.equals(forumConfig.getIdlePostEnabled())) {
                return;
            }
            Long botUserId = aiIdentityService.getForumBotUserId();
            if (botUserId == null || botUserId <= 0L) {
                return;
            }
            if (!isForumIdle(resolveIdlePostTriggerSeconds(forumConfig.getIdlePostTriggerSeconds()))) {
                return;
            }
            if (!hitProbability(forumConfig.getIdlePostProbability(), DEFAULT_IDLE_POST_PROBABILITY)) {
                return;
            }
            if (!aiRateLimitService.allowByCooldown(FORUM_POST_SCENE, botUserId, "global", forumConfig.getCooldownSeconds())) {
                return;
            }
            if (!aiRateLimitService.allowByHourlyLimit(
                    FORUM_POST_SCENE,
                    botUserId,
                    resolveMaxPostsPerHour(forumConfig.getMaxPostsPerHour()))) {
                return;
            }
            if (!aiRateLimitService.allowByDailyLimit(
                    FORUM_POST_SCENE,
                    botUserId,
                    resolveMaxPostsPerDay(forumConfig.getMaxPostsPerDay()))) {
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
                    forumConfig.getMinDelaySeconds(),
                    forumConfig.getMaxDelaySeconds(),
                    () -> processIdlePost(botUserId, forumConfig)
            );
        } catch (Exception ex) {
            log.warn("AI 论坛冷场发帖任务异常: {}", ex.getMessage());
        }
    }

    private void processIdlePost(Long botUserId, AiConfig.Forum forumConfig) {
        try {
            if (!isForumIdle(resolveIdlePostTriggerSeconds(forumConfig.getIdlePostTriggerSeconds()))) {
                return;
            }

            List<WfForumThread> recentThreads = loadRecentVisibleThreads(resolveContextLimit());
            AiRoleService.AiRoleProfile roleProfile = aiRoleService.pickRole(FORUM_SCENE);
            String rolePrompt = buildRolePrompt(roleProfile);
            String moodDirective = aiMoodService.buildMoodDirective(FORUM_SCENE, botUserId, "论坛冷场开帖");
            String memoryDigest = aiMemoryDigestService.buildForumThreadDigest(recentThreads);
            memoryDigest = mergeMemoryDigest(
                    memoryDigest,
                    aiInteractionMemoryService.buildTopicDigest(FORUM_SCENE, botUserId, 3)
            );
            String prompt = aiPromptBuilderService.buildForumThreadPrompt(
                    botUserId,
                    recentThreads,
                    rolePrompt,
                    moodDirective,
                    memoryDigest
            );
            String systemPrompt = normalizeSystemPrompt(forumConfig.getSystemPrompt());
            systemPrompt = appendRoleSystemPrompt(systemPrompt, roleProfile);

            ThreadDraft draft = parseThreadDraft(
                    aiTextClient.complete(systemPrompt, prompt),
                    forumConfig.getMaxPostTitleChars(),
                    forumConfig.getMaxPostContentChars()
            );
            if (draft == null) {
                return;
            }

            CreateThreadDTO dto = new CreateThreadDTO();
            dto.setTitle(draft.title);
            dto.setContent(draft.content);
            userPresenceService.markActive(botUserId);
            forumService.createThread(botUserId, dto);
            log.info("AI 论坛冷场发帖完成: botUserId={}, title={}, role={}",
                    botUserId, draft.title, roleCode(roleProfile));
        } catch (Exception ex) {
            log.warn("AI 论坛冷场发帖失败: botUserId={}, message={}", botUserId, ex.getMessage());
        }
    }

    private boolean isRuntimeReady() {
        return aiIdentityService.isAiEnabled() && aiTextClient.isAvailable();
    }

    private boolean isForumIdle(int idleTriggerSeconds) {
        LambdaQueryWrapper<WfForumThread> queryWrapper = new LambdaQueryWrapper<>();
        queryWrapper.in(WfForumThread::getStatus, List.of(ForumThreadStatus.NORMAL, ForumThreadStatus.LOCKED))
                .orderByDesc(WfForumThread::getLastReplyTime)
                .orderByDesc(WfForumThread::getCreateTime)
                .orderByDesc(WfForumThread::getId)
                .last("LIMIT 1");
        WfForumThread latestThread = wfForumThreadMapper.selectOne(queryWrapper);
        if (latestThread == null) {
            return true;
        }
        LocalDateTime latestActiveAt = latestThread.getLastReplyTime() != null
                ? latestThread.getLastReplyTime()
                : latestThread.getCreateTime();
        if (latestActiveAt == null) {
            return true;
        }
        return latestActiveAt.isBefore(LocalDateTime.now().minusSeconds(idleTriggerSeconds));
    }

    private List<WfForumThread> loadRecentVisibleThreads(int contextLimit) {
        if (contextLimit <= 0) {
            return Collections.emptyList();
        }
        LambdaQueryWrapper<WfForumThread> queryWrapper = new LambdaQueryWrapper<>();
        queryWrapper.in(WfForumThread::getStatus, List.of(ForumThreadStatus.NORMAL, ForumThreadStatus.LOCKED))
                .orderByDesc(WfForumThread::getLastReplyTime)
                .orderByDesc(WfForumThread::getCreateTime)
                .orderByDesc(WfForumThread::getId)
                .last("LIMIT " + contextLimit);
        List<WfForumThread> rows = wfForumThreadMapper.selectList(queryWrapper);
        if (rows.isEmpty()) {
            return rows;
        }
        Collections.reverse(rows);
        return rows;
    }

    private ThreadDraft parseThreadDraft(String raw, Integer configuredTitleChars, Integer configuredContentChars) {
        if (!StringUtils.hasText(raw)) {
            return null;
        }
        String normalized = raw.replace("\r", "").trim();
        if (!StringUtils.hasText(normalized)) {
            return null;
        }

        String title = null;
        StringBuilder contentBuilder = new StringBuilder();
        String[] lines = normalized.split("\n");
        for (String line : lines) {
            String trimmed = line == null ? "" : line.trim();
            if (!StringUtils.hasText(trimmed)) {
                continue;
            }
            if (trimmed.startsWith("标题：") || trimmed.startsWith("标题:")) {
                title = extractLabeledText(trimmed);
                continue;
            }
            if (trimmed.startsWith("正文：") || trimmed.startsWith("正文:")) {
                appendLine(contentBuilder, extractLabeledText(trimmed));
                continue;
            }
            if (!StringUtils.hasText(title)) {
                title = trimmed;
            } else {
                appendLine(contentBuilder, trimmed);
            }
        }

        int maxTitleChars = resolveMaxTitleChars(configuredTitleChars);
        int maxContentChars = resolveMaxContentChars(configuredContentChars);
        String normalizedContent = normalizeText(contentBuilder.toString(), maxContentChars);
        String normalizedTitle = normalizeText(title, maxTitleChars);
        if (!StringUtils.hasText(normalizedContent)) {
            return null;
        }
        if (!StringUtils.hasText(normalizedTitle)) {
            normalizedTitle = buildFallbackTitle(normalizedContent, maxTitleChars);
        }
        if (!StringUtils.hasText(normalizedTitle)) {
            return null;
        }
        return new ThreadDraft(normalizedTitle, normalizedContent);
    }

    private String extractLabeledText(String line) {
        int index = line.indexOf('：');
        if (index < 0) {
            index = line.indexOf(':');
        }
        if (index < 0 || index >= line.length() - 1) {
            return "";
        }
        return line.substring(index + 1).trim();
    }

    private void appendLine(StringBuilder builder, String line) {
        if (!StringUtils.hasText(line)) {
            return;
        }
        if (builder.length() > 0) {
            builder.append('\n');
        }
        builder.append(line.trim());
    }

    private String normalizeText(String text, int maxChars) {
        if (!StringUtils.hasText(text)) {
            return "";
        }
        String normalized = text.trim();
        if (normalized.length() <= maxChars) {
            return normalized;
        }
        return normalized.substring(0, maxChars);
    }

    private String buildFallbackTitle(String content, int maxTitleChars) {
        if (!StringUtils.hasText(content)) {
            return "";
        }
        String compact = content.replace('\n', ' ').trim();
        if (!StringUtils.hasText(compact)) {
            return "";
        }
        int cutLength = Math.min(maxTitleChars, compact.length());
        return compact.substring(0, cutLength);
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

    private int resolveIdlePostTriggerSeconds(Integer configured) {
        if (configured == null || configured <= 0) {
            return DEFAULT_IDLE_TRIGGER_SECONDS;
        }
        return Math.max(300, Math.min(configured, 7200));
    }

    private int resolveContextLimit() {
        Integer configured = aiConfig.getMemory() == null ? null : aiConfig.getMemory().getRecentWindowSize();
        if (configured == null || configured <= 0) {
            return DEFAULT_CONTEXT_LIMIT;
        }
        return Math.max(6, Math.min(configured, 30));
    }

    private int resolveMaxPostsPerHour(Integer configured) {
        if (configured == null || configured <= 0) {
            return DEFAULT_MAX_POSTS_PER_HOUR;
        }
        return Math.max(1, Math.min(configured, 20));
    }

    private int resolveMaxPostsPerDay(Integer configured) {
        if (configured == null || configured <= 0) {
            return DEFAULT_MAX_POSTS_PER_DAY;
        }
        return Math.max(1, Math.min(configured, 100));
    }

    private int resolveMaxTitleChars(Integer configured) {
        if (configured == null || configured <= 0) {
            return DEFAULT_MAX_TITLE_CHARS;
        }
        return Math.max(12, Math.min(configured, 120));
    }

    private int resolveMaxContentChars(Integer configured) {
        if (configured == null || configured <= 0) {
            return DEFAULT_MAX_CONTENT_CHARS;
        }
        return Math.max(60, Math.min(configured, 1200));
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

    private String normalizeSystemPrompt(String configuredPrompt) {
        if (StringUtils.hasText(configuredPrompt)) {
            return configuredPrompt.trim();
        }
        return "你在论坛发主题，请像普通用户自然发帖，不刷屏。";
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

    private String roleCode(AiRoleService.AiRoleProfile roleProfile) {
        if (roleProfile == null || !StringUtils.hasText(roleProfile.getRoleCode())) {
            return "default";
        }
        return roleProfile.getRoleCode();
    }

    private String mergeMemoryDigest(String first, String second) {
        if (!StringUtils.hasText(first)) {
            return second;
        }
        if (!StringUtils.hasText(second)) {
            return first;
        }
        return first + "\n" + second;
    }

    private static class ThreadDraft {
        private final String title;
        private final String content;

        private ThreadDraft(String title, String content) {
            this.title = title;
            this.content = content;
        }
    }
}
