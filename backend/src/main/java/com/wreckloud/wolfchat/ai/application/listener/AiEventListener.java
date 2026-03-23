package com.wreckloud.wolfchat.ai.application.listener;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.wreckloud.wolfchat.account.application.service.UserService;
import com.wreckloud.wolfchat.account.domain.entity.WfUser;
import com.wreckloud.wolfchat.ai.application.client.AiTextClient;
import com.wreckloud.wolfchat.ai.application.service.AiIdentityService;
import com.wreckloud.wolfchat.ai.application.service.AiInteractionMemoryService;
import com.wreckloud.wolfchat.ai.application.service.AiMemoryDigestService;
import com.wreckloud.wolfchat.ai.application.service.AiMoodService;
import com.wreckloud.wolfchat.ai.application.service.AiPromptBuilderService;
import com.wreckloud.wolfchat.ai.application.service.AiRateLimitService;
import com.wreckloud.wolfchat.ai.application.service.AiRoleService;
import com.wreckloud.wolfchat.ai.application.service.AiTaskSchedulerService;
import com.wreckloud.wolfchat.ai.application.support.AiLobbyDialogueIntentSupport;
import com.wreckloud.wolfchat.ai.config.AiConfig;
import com.wreckloud.wolfchat.chat.lobby.application.command.SendLobbyMessageCommand;
import com.wreckloud.wolfchat.chat.lobby.application.event.LobbyMessageSentEvent;
import com.wreckloud.wolfchat.chat.lobby.application.service.LobbyService;
import com.wreckloud.wolfchat.chat.lobby.domain.entity.WfLobbyMessage;
import com.wreckloud.wolfchat.chat.lobby.infra.mapper.WfLobbyMessageMapper;
import com.wreckloud.wolfchat.chat.message.application.command.SendMessageCommand;
import com.wreckloud.wolfchat.chat.message.application.event.PrivateMessageSentEvent;
import com.wreckloud.wolfchat.chat.message.application.service.ChatMessagePushService;
import com.wreckloud.wolfchat.chat.message.application.service.MessageService;
import com.wreckloud.wolfchat.chat.message.domain.entity.WfMessage;
import com.wreckloud.wolfchat.chat.message.domain.enums.MessageType;
import com.wreckloud.wolfchat.chat.message.infra.mapper.WfMessageMapper;
import com.wreckloud.wolfchat.chat.presence.application.service.UserPresenceService;
import com.wreckloud.wolfchat.common.excption.BaseException;
import com.wreckloud.wolfchat.community.api.dto.CreateReplyDTO;
import com.wreckloud.wolfchat.community.application.event.ForumReplyCreatedEvent;
import com.wreckloud.wolfchat.community.application.event.ForumThreadCreatedEvent;
import com.wreckloud.wolfchat.community.application.service.ForumService;
import com.wreckloud.wolfchat.community.domain.entity.WfForumReply;
import com.wreckloud.wolfchat.community.domain.entity.WfForumThread;
import com.wreckloud.wolfchat.community.domain.enums.ForumReplyStatus;
import com.wreckloud.wolfchat.community.domain.enums.ForumThreadStatus;
import com.wreckloud.wolfchat.community.infra.mapper.WfForumReplyMapper;
import com.wreckloud.wolfchat.community.infra.mapper.WfForumThreadMapper;
import com.wreckloud.wolfchat.follow.application.event.UserFollowedEvent;
import com.wreckloud.wolfchat.follow.application.service.FollowService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

import java.time.LocalDateTime;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * AI 业务事件监听器。
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class AiEventListener {
    private static final int DEFAULT_CONTEXT_LIMIT = 20;
    private static final int MAX_CONTEXT_LIMIT = 40;
    private static final String PRIVATE_SCENE = "private";
    private static final String LOBBY_SCENE = "lobby";
    private static final String LOBBY_MENTION_SCENE = "lobby-mention";
    private static final String FORUM_SCENE = "forum";
    private static final String FORUM_MENTION_SCENE = "forum-mention";
    private static final int DEFAULT_PRIVATE_MAX_REPLY_CHARS = 180;
    private static final int DEFAULT_LOBBY_MAX_REPLY_CHARS = 140;
    private static final int DEFAULT_FORUM_MAX_REPLY_CHARS = 220;
    private static final double DEFAULT_MENTION_REPLY_PROBABILITY = 0.85D;

    private static final String DEFAULT_PRIVATE_SYSTEM_PROMPT = "你是 WolfChat 的真人用户，请自然回复，短句优先。禁止自称AI。";
    private static final String DEFAULT_LOBBY_SYSTEM_PROMPT = "你在公共聊天室聊天，请像普通网友一样自然参与，不要刷屏。";
    private static final String DEFAULT_FORUM_SYSTEM_PROMPT = "你在社区讨论区回帖，请围绕主题给出简洁、有信息量的回复。";

    private final AiConfig aiConfig;
    private final AiTextClient aiTextClient;
    private final AiIdentityService aiIdentityService;
    private final AiRateLimitService aiRateLimitService;
    private final AiRoleService aiRoleService;
    private final AiMoodService aiMoodService;
    private final AiMemoryDigestService aiMemoryDigestService;
    private final AiInteractionMemoryService aiInteractionMemoryService;
    private final AiTaskSchedulerService aiTaskSchedulerService;
    private final AiPromptBuilderService aiPromptBuilderService;
    private final AiLobbyDialogueIntentSupport aiLobbyDialogueIntentSupport;
    private final UserService userService;
    private final MessageService messageService;
    private final LobbyService lobbyService;
    private final ForumService forumService;
    private final FollowService followService;
    private final ChatMessagePushService chatMessagePushService;
    private final UserPresenceService userPresenceService;
    private final WfMessageMapper wfMessageMapper;
    private final WfLobbyMessageMapper wfLobbyMessageMapper;
    private final WfForumThreadMapper wfForumThreadMapper;
    private final WfForumReplyMapper wfForumReplyMapper;

    @EventListener
    public void onPrivateMessageSent(PrivateMessageSentEvent event) {
        if (event == null || !isRuntimeReady()) {
            return;
        }
        AiConfig.PrivateChat privateChatConfig = aiConfig.getPrivateChat();
        if (privateChatConfig == null || !Boolean.TRUE.equals(privateChatConfig.getEnabled())) {
            return;
        }
        Long botUserId = aiIdentityService.getPrivateBotUserId();
        if (!isValidUserId(botUserId) || !botUserId.equals(event.getReceiverId())) {
            return;
        }
        if (aiIdentityService.isAiUser(event.getSenderId()) || isUnsupportedTriggerType(event.getMsgType())) {
            return;
        }
        aiInteractionMemoryService.recordInteraction(PRIVATE_SCENE, botUserId, event.getSenderId(), event.getContent());
        String dedupKey = PRIVATE_SCENE + ":" + event.getConversationId();
        if (aiTaskSchedulerService.isPending(dedupKey)) {
            return;
        }
        if (!hitProbability(privateChatConfig.getReplyProbability(), 0.85D)) {
            return;
        }
        if (!aiRateLimitService.allowByCooldown(PRIVATE_SCENE, botUserId, String.valueOf(event.getConversationId()),
                privateChatConfig.getCooldownSeconds())) {
            return;
        }
        if (!aiRateLimitService.allowByHourlyLimit(PRIVATE_SCENE, botUserId, privateChatConfig.getMaxRepliesPerHour())) {
            return;
        }
        if (!allowGlobalQuota(PRIVATE_SCENE, botUserId)) {
            return;
        }

        boolean scheduled = aiTaskSchedulerService.schedule(
                dedupKey,
                privateChatConfig.getMinDelaySeconds(),
                privateChatConfig.getMaxDelaySeconds(),
                () -> processPrivateReply(event, botUserId, privateChatConfig)
        );
        if (!scheduled) {
            log.debug("AI 私聊触发跳过: dedupKey={}", dedupKey);
        }
    }

    @EventListener
    public void onLobbyMessageSent(LobbyMessageSentEvent event) {
        if (event == null || !isRuntimeReady()) {
            return;
        }
        AiConfig.Lobby lobbyConfig = aiConfig.getLobby();
        if (lobbyConfig == null || !Boolean.TRUE.equals(lobbyConfig.getEnabled())) {
            return;
        }
        Long botUserId = aiIdentityService.getLobbyBotUserId();
        if (!isValidUserId(botUserId) || botUserId.equals(event.getSenderId())) {
            return;
        }
        if (aiIdentityService.isAiUser(event.getSenderId()) || isUnsupportedTriggerType(event.getMsgType())) {
            return;
        }
        WfLobbyMessage triggerMessage = wfLobbyMessageMapper.selectById(event.getMessageId());
        boolean directedToBot = isDirectedLobbyMessage(event, triggerMessage, botUserId);
        aiInteractionMemoryService.recordInteraction(LOBBY_SCENE, botUserId, event.getSenderId(), event.getContent());
        String dedupKey = directedToBot
                ? LOBBY_MENTION_SCENE + ":" + botUserId + ":" + event.getSenderId()
                : LOBBY_SCENE + ":" + botUserId;
        if (aiTaskSchedulerService.isPending(dedupKey)) {
            return;
        }
        double defaultProbability = directedToBot ? DEFAULT_MENTION_REPLY_PROBABILITY : 0.25D;
        Double configuredProbability = directedToBot
                ? lobbyConfig.getMentionReplyProbability()
                : lobbyConfig.getReplyProbability();
        if (!hitProbability(configuredProbability, defaultProbability)) {
            return;
        }
        String cooldownScene = directedToBot ? LOBBY_MENTION_SCENE : LOBBY_SCENE;
        String cooldownTarget = directedToBot ? String.valueOf(event.getSenderId()) : "global";
        if (!aiRateLimitService.allowByCooldown(cooldownScene, botUserId, cooldownTarget, lobbyConfig.getCooldownSeconds())) {
            return;
        }
        if (!aiRateLimitService.allowByHourlyLimit(LOBBY_SCENE, botUserId, lobbyConfig.getMaxRepliesPerHour())) {
            return;
        }
        if (!allowGlobalQuota(LOBBY_SCENE, botUserId)) {
            return;
        }

        int minDelaySeconds = directedToBot
                ? resolveMentionMinDelaySeconds(lobbyConfig.getMentionMinDelaySeconds(), lobbyConfig.getMinDelaySeconds())
                : lobbyConfig.getMinDelaySeconds();
        int maxDelaySeconds = directedToBot
                ? resolveMentionMaxDelaySeconds(lobbyConfig.getMentionMaxDelaySeconds(), lobbyConfig.getMaxDelaySeconds(), minDelaySeconds)
                : lobbyConfig.getMaxDelaySeconds();
        boolean scheduled = aiTaskSchedulerService.schedule(
                dedupKey,
                minDelaySeconds,
                maxDelaySeconds,
                () -> processLobbyReply(event, triggerMessage, botUserId, lobbyConfig, directedToBot)
        );
        if (!scheduled) {
            log.debug("AI 大厅触发跳过: dedupKey={}", dedupKey);
        }
    }

    @EventListener
    public void onForumThreadCreated(ForumThreadCreatedEvent event) {
        if (event == null || !isRuntimeReady()) {
            return;
        }
        AiConfig.Forum forumConfig = aiConfig.getForum();
        if (forumConfig == null || !Boolean.TRUE.equals(forumConfig.getEnabled())) {
            return;
        }
        Long botUserId = aiIdentityService.getForumBotUserId();
        if (!isValidUserId(botUserId) || botUserId.equals(event.getAuthorId())) {
            return;
        }
        if (aiIdentityService.isAiUser(event.getAuthorId())) {
            return;
        }
        boolean directedToBot = containsAiMention(botUserId, event.getTitle(), event.getContent());
        aiInteractionMemoryService.recordInteraction(
                FORUM_SCENE,
                botUserId,
                event.getAuthorId(),
                buildForumInteractionText(event.getTitle(), event.getContent())
        );
        String dedupKey = directedToBot
                ? FORUM_MENTION_SCENE + ":thread:" + event.getThreadId()
                : FORUM_SCENE + ":thread:" + event.getThreadId();
        if (aiTaskSchedulerService.isPending(dedupKey)) {
            return;
        }
        double defaultProbability = directedToBot ? DEFAULT_MENTION_REPLY_PROBABILITY : 0.28D;
        Double configuredProbability = directedToBot
                ? forumConfig.getMentionReplyProbability()
                : forumConfig.getReplyProbability();
        if (!hitProbability(configuredProbability, defaultProbability)) {
            return;
        }
        String cooldownScene = directedToBot ? FORUM_MENTION_SCENE : FORUM_SCENE;
        if (!aiRateLimitService.allowByCooldown(cooldownScene, botUserId, String.valueOf(event.getThreadId()), forumConfig.getCooldownSeconds())) {
            return;
        }
        if (!aiRateLimitService.allowByHourlyLimit(FORUM_SCENE, botUserId, forumConfig.getMaxRepliesPerHour())) {
            return;
        }
        if (!aiRateLimitService.allowByDailyLimit(FORUM_SCENE, botUserId, forumConfig.getMaxRepliesPerDay())) {
            return;
        }
        if (!allowGlobalQuota(FORUM_SCENE, botUserId)) {
            return;
        }

        int minDelaySeconds = directedToBot
                ? resolveMentionMinDelaySeconds(forumConfig.getMentionMinDelaySeconds(), forumConfig.getMinDelaySeconds())
                : forumConfig.getMinDelaySeconds();
        int maxDelaySeconds = directedToBot
                ? resolveMentionMaxDelaySeconds(forumConfig.getMentionMaxDelaySeconds(), forumConfig.getMaxDelaySeconds(), minDelaySeconds)
                : forumConfig.getMaxDelaySeconds();
        boolean scheduled = aiTaskSchedulerService.schedule(
                dedupKey,
                minDelaySeconds,
                maxDelaySeconds,
                () -> processForumReply(event.getThreadId(), null, botUserId, forumConfig, directedToBot)
        );
        if (!scheduled) {
            log.debug("AI 论坛主题触发跳过: dedupKey={}", dedupKey);
        }
    }

    @EventListener
    public void onForumReplyCreated(ForumReplyCreatedEvent event) {
        if (event == null || !isRuntimeReady()) {
            return;
        }
        AiConfig.Forum forumConfig = aiConfig.getForum();
        if (forumConfig == null || !Boolean.TRUE.equals(forumConfig.getEnabled())) {
            return;
        }
        Long botUserId = aiIdentityService.getForumBotUserId();
        if (!isValidUserId(botUserId) || botUserId.equals(event.getAuthorId())) {
            return;
        }
        if (aiIdentityService.isAiUser(event.getAuthorId())) {
            return;
        }
        WfForumReply quoteReply = event.getQuoteReplyId() == null ? null : wfForumReplyMapper.selectById(event.getQuoteReplyId());
        boolean directedToBot = isDirectedForumReply(event, quoteReply, botUserId);
        aiInteractionMemoryService.recordInteraction(FORUM_SCENE, botUserId, event.getAuthorId(), event.getContent());
        String dedupKey = directedToBot
                ? FORUM_MENTION_SCENE + ":reply:" + event.getThreadId() + ":" + event.getAuthorId()
                : FORUM_SCENE + ":reply:" + event.getThreadId();
        if (aiTaskSchedulerService.isPending(dedupKey)) {
            return;
        }
        double defaultProbability = directedToBot ? DEFAULT_MENTION_REPLY_PROBABILITY : 0.20D;
        Double configuredProbability = directedToBot
                ? forumConfig.getMentionReplyProbability()
                : forumConfig.getReplyToReplyProbability();
        if (!hitProbability(configuredProbability, defaultProbability)) {
            return;
        }
        String cooldownScene = directedToBot ? FORUM_MENTION_SCENE : FORUM_SCENE;
        if (!aiRateLimitService.allowByCooldown(cooldownScene, botUserId, String.valueOf(event.getThreadId()), forumConfig.getCooldownSeconds())) {
            return;
        }
        if (!aiRateLimitService.allowByHourlyLimit(FORUM_SCENE, botUserId, forumConfig.getMaxRepliesPerHour())) {
            return;
        }
        if (!aiRateLimitService.allowByDailyLimit(FORUM_SCENE, botUserId, forumConfig.getMaxRepliesPerDay())) {
            return;
        }
        if (!allowGlobalQuota(FORUM_SCENE, botUserId)) {
            return;
        }

        int minDelaySeconds = directedToBot
                ? resolveMentionMinDelaySeconds(forumConfig.getMentionMinDelaySeconds(), forumConfig.getMinDelaySeconds())
                : forumConfig.getMinDelaySeconds();
        int maxDelaySeconds = directedToBot
                ? resolveMentionMaxDelaySeconds(forumConfig.getMentionMaxDelaySeconds(), forumConfig.getMaxDelaySeconds(), minDelaySeconds)
                : forumConfig.getMaxDelaySeconds();
        boolean scheduled = aiTaskSchedulerService.schedule(
                dedupKey,
                minDelaySeconds,
                maxDelaySeconds,
                () -> processForumReply(event.getThreadId(), event.getReplyId(), botUserId, forumConfig, directedToBot)
        );
        if (!scheduled) {
            log.debug("AI 论坛回帖触发跳过: dedupKey={}", dedupKey);
        }
    }

    @EventListener
    public void onUserFollowed(UserFollowedEvent event) {
        if (event == null || !isRuntimeReady()) {
            return;
        }
        AiConfig.Follow followConfig = aiConfig.getFollow();
        if (followConfig == null || !Boolean.TRUE.equals(followConfig.getAutoFollowBackEnabled())) {
            return;
        }
        if (!aiIdentityService.isAiUser(event.getFolloweeId()) || aiIdentityService.isAiUser(event.getFollowerId())) {
            return;
        }
        String dedupKey = "follow:" + event.getFolloweeId() + ":" + event.getFollowerId();
        aiTaskSchedulerService.schedule(
                dedupKey,
                followConfig.getMinDelaySeconds(),
                followConfig.getMaxDelaySeconds(),
                () -> processAutoFollowBack(event.getFolloweeId(), event.getFollowerId())
        );
    }

    private void processPrivateReply(PrivateMessageSentEvent event, Long botUserId, AiConfig.PrivateChat privateChatConfig) {
        try {
            AiRoleService.AiRoleProfile roleProfile = aiRoleService.pickRole(PRIVATE_SCENE);
            String rolePrompt = buildRolePrompt(roleProfile);
            List<WfMessage> recentMessages = loadRecentPrivateMessages(event.getConversationId());
            String moodDirective = aiMoodService.buildMoodDirective(PRIVATE_SCENE, botUserId, event.getContent());
            String memoryDigest = aiMemoryDigestService.buildPrivateDigest(recentMessages);
            memoryDigest = mergeMemoryDigest(memoryDigest, buildInteractionMemoryDigest(PRIVATE_SCENE, botUserId));
            String prompt = aiPromptBuilderService.buildPrivatePrompt(
                    botUserId, event.getSenderId(), recentMessages, rolePrompt, moodDirective, memoryDigest
            );
            String systemPrompt = normalizeSystemPrompt(privateChatConfig.getSystemPrompt(), DEFAULT_PRIVATE_SYSTEM_PROMPT);
            systemPrompt = appendRoleSystemPrompt(systemPrompt, roleProfile);
            log.debug("AI 私聊推理: conversationId={}, contextSize={}, promptChars={}",
                    event.getConversationId(), recentMessages.size(), prompt.length());
            String reply = normalizeAiReply(
                    aiTextClient.complete(systemPrompt, prompt),
                    privateChatConfig.getMaxReplyChars(),
                    DEFAULT_PRIVATE_MAX_REPLY_CHARS
            );
            if (!StringUtils.hasText(reply)) {
                return;
            }

            SendMessageCommand command = new SendMessageCommand();
            command.setUserId(botUserId);
            command.setConversationId(event.getConversationId());
            command.setMsgType(MessageType.TEXT);
            command.setContent(reply);
            userPresenceService.markOnline(botUserId);
            WfMessage message = messageService.sendMessage(command);
            chatMessagePushService.pushPrivateMessageToReceiver(message);
            log.info("AI 私聊回复完成: conversationId={}, messageId={}, role={}",
                    event.getConversationId(), message.getId(), roleCode(roleProfile));
        } catch (Exception ex) {
            log.warn("AI 私聊回复失败: conversationId={}, message={}", event.getConversationId(), ex.getMessage());
        }
    }

    private void processLobbyReply(LobbyMessageSentEvent event,
                                   WfLobbyMessage triggerMessage,
                                   Long botUserId,
                                   AiConfig.Lobby lobbyConfig,
                                   boolean directedToBot) {
        try {
            AiRoleService.AiRoleProfile roleProfile = aiRoleService.pickRole(LOBBY_SCENE);
            String rolePrompt = buildRolePrompt(roleProfile);
            List<WfLobbyMessage> recentMessages = loadRecentLobbyMessages();
            String moodDirective = aiMoodService.buildMoodDirective(LOBBY_SCENE, botUserId, event.getContent());
            String memoryDigest = aiMemoryDigestService.buildLobbyDigest(recentMessages);
            memoryDigest = mergeMemoryDigest(memoryDigest, buildInteractionMemoryDigest(LOBBY_SCENE, botUserId));
            String prompt = aiPromptBuilderService.buildLobbyPrompt(
                    botUserId, event.getSenderId(), recentMessages, rolePrompt, moodDirective, memoryDigest
            );
            if (directedToBot) {
                String targetName = resolveUserDisplayName(event.getSenderId());
                prompt = prompt + "\n触发说明：对方在点名你或回复你，请优先正面回应对方。";
                if (StringUtils.hasText(targetName)) {
                    prompt = prompt + "\n可直接称呼对方：@" + targetName;
                }
            }
            String systemPrompt = normalizeSystemPrompt(lobbyConfig.getSystemPrompt(), DEFAULT_LOBBY_SYSTEM_PROMPT);
            systemPrompt = appendRoleSystemPrompt(systemPrompt, roleProfile);
            log.debug("AI 大厅推理: triggerMessageId={}, contextSize={}, promptChars={}",
                    event.getMessageId(), recentMessages.size(), prompt.length());
            String reply = normalizeAiReply(
                    aiTextClient.complete(systemPrompt, prompt),
                    lobbyConfig.getMaxReplyChars(),
                    DEFAULT_LOBBY_MAX_REPLY_CHARS
            );
            if (!StringUtils.hasText(reply)) {
                return;
            }
            if (directedToBot) {
                reply = ensureReplyMention(reply, resolveUserDisplayName(event.getSenderId()));
            }
            SendLobbyMessageCommand command = new SendLobbyMessageCommand();
            command.setUserId(botUserId);
            command.setMsgType(MessageType.TEXT);
            command.setContent(reply);
            if (directedToBot && triggerMessage != null && triggerMessage.getId() != null) {
                command.setReplyToMessageId(triggerMessage.getId());
            }
            userPresenceService.markOnline(botUserId);
            chatMessagePushService.pushLobbyMessage(botUserId, lobbyService.sendMessage(command));
            log.info("AI 大厅回复完成: triggerMessageId={}, role={}",
                    event.getMessageId(), roleCode(roleProfile));
        } catch (Exception ex) {
            log.warn("AI 大厅回复失败: triggerMessageId={}, message={}", event.getMessageId(), ex.getMessage());
        }
    }

    private void processForumReply(Long threadId,
                                   Long quoteReplyId,
                                   Long botUserId,
                                   AiConfig.Forum forumConfig,
                                   boolean directedToBot) {
        try {
            AiRoleService.AiRoleProfile roleProfile = aiRoleService.pickRole(FORUM_SCENE);
            String rolePrompt = buildRolePrompt(roleProfile);
            WfForumThread thread = wfForumThreadMapper.selectById(threadId);
            if (thread == null || !isVisibleThreadStatus(thread.getStatus())) {
                return;
            }
            WfForumReply triggerReply = quoteReplyId == null ? null : wfForumReplyMapper.selectById(quoteReplyId);
            List<WfForumReply> recentReplies = loadRecentForumReplies(threadId);
            String triggerText = triggerReply == null ? thread.getContent() : triggerReply.getContent();
            String moodDirective = aiMoodService.buildMoodDirective(FORUM_SCENE, botUserId, triggerText);
            String memoryDigest = aiMemoryDigestService.buildForumDigest(recentReplies);
            memoryDigest = mergeMemoryDigest(memoryDigest, buildInteractionMemoryDigest(FORUM_SCENE, botUserId));
            String prompt = aiPromptBuilderService.buildForumReplyPrompt(
                    botUserId, thread, triggerReply, recentReplies, rolePrompt, moodDirective, memoryDigest
            );
            if (directedToBot) {
                Long directedUserId = triggerReply == null ? thread.getAuthorId() : triggerReply.getAuthorId();
                String targetName = resolveUserDisplayName(directedUserId);
                prompt = prompt + "\n触发说明：有人在点名你或直接回复你，请优先回应对方。";
                if (StringUtils.hasText(targetName)) {
                    prompt = prompt + "\n建议称呼：@" + targetName;
                }
            }
            String systemPrompt = normalizeSystemPrompt(forumConfig.getSystemPrompt(), DEFAULT_FORUM_SYSTEM_PROMPT);
            systemPrompt = appendRoleSystemPrompt(systemPrompt, roleProfile);
            log.debug("AI 论坛推理: threadId={}, quoteReplyId={}, contextSize={}, promptChars={}",
                    threadId, quoteReplyId, recentReplies.size(), prompt.length());
            String reply = normalizeAiReply(
                    aiTextClient.complete(systemPrompt, prompt),
                    forumConfig.getMaxReplyChars(),
                    DEFAULT_FORUM_MAX_REPLY_CHARS
            );
            if (!StringUtils.hasText(reply)) {
                return;
            }
            if (directedToBot) {
                Long directedUserId = triggerReply == null ? thread.getAuthorId() : triggerReply.getAuthorId();
                reply = ensureForumDirectedPrefix(reply, resolveUserDisplayName(directedUserId));
            }

            CreateReplyDTO dto = new CreateReplyDTO();
            dto.setContent(reply);
            if (quoteReplyId != null && quoteReplyId > 0L && triggerReply != null && ForumReplyStatus.NORMAL.equals(triggerReply.getStatus())) {
                dto.setQuoteReplyId(quoteReplyId);
            }
            userPresenceService.markActive(botUserId);
            forumService.createReply(botUserId, threadId, dto);
            log.info("AI 论坛回复完成: threadId={}, quoteReplyId={}, role={}",
                    threadId, quoteReplyId, roleCode(roleProfile));
        } catch (Exception ex) {
            log.warn("AI 论坛回复失败: threadId={}, message={}", threadId, ex.getMessage());
        }
    }

    private void processAutoFollowBack(Long botUserId, Long targetUserId) {
        try {
            userPresenceService.markActive(botUserId);
            followService.follow(botUserId, targetUserId);
            log.info("AI 自动回关完成: botUserId={}, targetUserId={}", botUserId, targetUserId);
        } catch (BaseException ex) {
            // 已关注、禁用等业务态直接忽略
            log.debug("AI 自动回关跳过: botUserId={}, targetUserId={}, code={}", botUserId, targetUserId, ex.getCode());
        } catch (Exception ex) {
            log.warn("AI 自动回关失败: botUserId={}, targetUserId={}, message={}", botUserId, targetUserId, ex.getMessage());
        }
    }

    private boolean isRuntimeReady() {
        return aiIdentityService.isAiEnabled() && aiTextClient.isAvailable();
    }

    private boolean allowGlobalQuota(String scene, Long botUserId) {
        AiConfig.Guard guard = aiConfig.getGuard();
        if (guard == null || !Boolean.TRUE.equals(guard.getEnabled())) {
            return true;
        }
        if (!aiRateLimitService.allowByGlobalHourlyLimit(botUserId, guard.getMaxCallsPerHour())) {
            log.info("AI 全局小时配额已达上限: scene={}, botUserId={}", scene, botUserId);
            return false;
        }
        if (!aiRateLimitService.allowByGlobalDailyLimit(botUserId, guard.getMaxCallsPerDay())) {
            log.warn("AI 全局日配额已达上限: scene={}, botUserId={}", scene, botUserId);
            return false;
        }
        return true;
    }

    private boolean isValidUserId(Long userId) {
        return userId != null && userId > 0L;
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

    private boolean isUnsupportedTriggerType(MessageType msgType) {
        return MessageType.RECALL.equals(msgType) || MessageType.SYSTEM.equals(msgType);
    }

    private String normalizeSystemPrompt(String configuredPrompt, String fallbackPrompt) {
        if (StringUtils.hasText(configuredPrompt)) {
            return configuredPrompt.trim();
        }
        return fallbackPrompt;
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

    private String normalizeAiReply(String raw, Integer configuredMaxChars, int defaultMaxChars) {
        if (!StringUtils.hasText(raw)) {
            return null;
        }
        String normalized = raw.replace("\r", "").trim();
        if (!StringUtils.hasText(normalized)) {
            return null;
        }
        int maxChars = resolveMaxReplyChars(configuredMaxChars, defaultMaxChars);
        if (normalized.length() <= maxChars) {
            return normalized;
        }
        return normalized.substring(0, maxChars);
    }

    private int resolveMaxReplyChars(Integer configuredMaxChars, int defaultMaxChars) {
        int safeDefault = defaultMaxChars <= 0 ? 180 : defaultMaxChars;
        if (configuredMaxChars == null || configuredMaxChars <= 0) {
            return safeDefault;
        }
        return Math.max(60, Math.min(configuredMaxChars, 600));
    }

    private List<WfMessage> loadRecentPrivateMessages(Long conversationId) {
        if (conversationId == null || conversationId <= 0L) {
            return Collections.emptyList();
        }
        int contextLimit = resolveContextLimit();
        LambdaQueryWrapper<WfMessage> queryWrapper = new LambdaQueryWrapper<>();
        queryWrapper.eq(WfMessage::getConversationId, conversationId)
                .notIn(WfMessage::getMsgType, List.of(MessageType.SYSTEM, MessageType.RECALL))
                .orderByDesc(WfMessage::getCreateTime)
                .orderByDesc(WfMessage::getId)
                .last("LIMIT " + contextLimit);
        List<WfMessage> rows = wfMessageMapper.selectList(queryWrapper);
        if (rows.isEmpty()) {
            return rows;
        }
        Collections.reverse(rows);
        return rows;
    }

    private List<WfLobbyMessage> loadRecentLobbyMessages() {
        int contextLimit = resolveContextLimit();
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

    private List<WfForumReply> loadRecentForumReplies(Long threadId) {
        if (threadId == null || threadId <= 0L) {
            return Collections.emptyList();
        }
        int contextLimit = resolveContextLimit();
        LambdaQueryWrapper<WfForumReply> queryWrapper = new LambdaQueryWrapper<>();
        queryWrapper.eq(WfForumReply::getThreadId, threadId)
                .eq(WfForumReply::getStatus, ForumReplyStatus.NORMAL)
                .orderByDesc(WfForumReply::getCreateTime)
                .orderByDesc(WfForumReply::getId)
                .last("LIMIT " + contextLimit);
        List<WfForumReply> rows = wfForumReplyMapper.selectList(queryWrapper);
        if (rows.isEmpty()) {
            return rows;
        }
        Collections.reverse(rows);
        return rows;
    }

    private int resolveContextLimit() {
        Integer configured = aiConfig.getMemory() == null ? null : aiConfig.getMemory().getRecentWindowSize();
        if (configured == null || configured <= 0) {
            return DEFAULT_CONTEXT_LIMIT;
        }
        return Math.min(configured, MAX_CONTEXT_LIMIT);
    }

    private boolean isDirectedLobbyMessage(LobbyMessageSentEvent event, WfLobbyMessage triggerMessage, Long botUserId) {
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
                this::resolveUserDisplayName
        );
    }

    private boolean isDirectedForumReply(ForumReplyCreatedEvent event, WfForumReply quoteReply, Long botUserId) {
        if (event == null || !isValidUserId(botUserId)) {
            return false;
        }
        if (quoteReply != null && botUserId.equals(quoteReply.getAuthorId())) {
            return true;
        }
        return containsAiMention(botUserId, event.getContent());
    }

    private boolean containsAiMention(Long aiUserId, String... texts) {
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

    private int resolveMentionMinDelaySeconds(Integer mentionMinDelaySeconds, Integer defaultDelaySeconds) {
        int fallback = defaultDelaySeconds == null || defaultDelaySeconds <= 0 ? 4 : defaultDelaySeconds;
        if (mentionMinDelaySeconds == null || mentionMinDelaySeconds <= 0) {
            return Math.max(2, Math.min(fallback, 12));
        }
        return Math.max(1, Math.min(mentionMinDelaySeconds, 20));
    }

    private int resolveMentionMaxDelaySeconds(Integer mentionMaxDelaySeconds, Integer defaultDelaySeconds, int minDelaySeconds) {
        int fallback = defaultDelaySeconds == null || defaultDelaySeconds <= 0
                ? Math.max(minDelaySeconds + 2, 8)
                : defaultDelaySeconds;
        int resolved = mentionMaxDelaySeconds == null || mentionMaxDelaySeconds <= 0
                ? Math.max(minDelaySeconds + 2, Math.min(fallback, 30))
                : Math.max(mentionMaxDelaySeconds, minDelaySeconds);
        return Math.max(minDelaySeconds, Math.min(resolved, 40));
    }

    private String buildForumInteractionText(String title, String content) {
        if (StringUtils.hasText(title) && StringUtils.hasText(content)) {
            return title.trim() + " " + content.trim();
        }
        if (StringUtils.hasText(title)) {
            return title.trim();
        }
        if (StringUtils.hasText(content)) {
            return content.trim();
        }
        return null;
    }

    private String buildInteractionMemoryDigest(String scene, Long botUserId) {
        List<Long> recentUserIds = aiInteractionMemoryService.listRecentUserIds(scene, botUserId, 3);
        String userDigest = buildRecentUserDigest(recentUserIds);
        String topicDigest = aiInteractionMemoryService.buildTopicDigest(scene, botUserId, 3);
        if (!StringUtils.hasText(userDigest) && !StringUtils.hasText(topicDigest)) {
            return null;
        }
        if (!StringUtils.hasText(userDigest)) {
            return topicDigest;
        }
        if (!StringUtils.hasText(topicDigest)) {
            return userDigest;
        }
        return userDigest + "\n" + topicDigest;
    }

    private String buildRecentUserDigest(List<Long> recentUserIds) {
        if (recentUserIds == null || recentUserIds.isEmpty()) {
            return null;
        }
        Map<Long, WfUser> userMap = userService.getUserMap(recentUserIds);
        StringBuilder builder = new StringBuilder();
        int appended = 0;
        for (Long userId : recentUserIds) {
            String name = resolveUserDisplayName(userMap.get(userId), userId);
            if (!StringUtils.hasText(name)) {
                continue;
            }
            if (appended > 0) {
                builder.append("、");
            }
            builder.append(name);
            appended++;
            if (appended >= 3) {
                break;
            }
        }
        if (builder.length() == 0) {
            return null;
        }
        return "最近常互动对象：" + builder + "。";
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

    private String ensureReplyMention(String reply, String targetName) {
        if (!StringUtils.hasText(reply) || !StringUtils.hasText(targetName)) {
            return reply;
        }
        String normalizedReply = reply.trim();
        String mention = "@" + targetName.trim();
        if (normalizedReply.startsWith(mention)) {
            return normalizedReply;
        }
        return mention + " " + normalizedReply;
    }

    private String ensureForumDirectedPrefix(String reply, String targetName) {
        if (!StringUtils.hasText(reply) || !StringUtils.hasText(targetName)) {
            return reply;
        }
        String normalizedReply = reply.trim();
        String prefix = "回复 @" + targetName.trim();
        if (normalizedReply.startsWith(prefix)) {
            return normalizedReply;
        }
        return prefix + "：" + normalizedReply;
    }

    private String resolveUserDisplayName(Long userId) {
        if (!isValidUserId(userId)) {
            return null;
        }
        Map<Long, WfUser> userMap = userService.getUserMap(Set.of(userId));
        return resolveUserDisplayName(userMap.get(userId), userId);
    }

    private String resolveUserDisplayName(WfUser user, Long fallbackUserId) {
        if (user != null && StringUtils.hasText(user.getNickname())) {
            return user.getNickname().trim();
        }
        if (user != null && StringUtils.hasText(user.getWolfNo())) {
            return user.getWolfNo().trim();
        }
        if (isValidUserId(fallbackUserId)) {
            return "user#" + fallbackUserId;
        }
        return null;
    }

    private boolean isVisibleThreadStatus(ForumThreadStatus status) {
        return ForumThreadStatus.NORMAL.equals(status) || ForumThreadStatus.LOCKED.equals(status);
    }
}
