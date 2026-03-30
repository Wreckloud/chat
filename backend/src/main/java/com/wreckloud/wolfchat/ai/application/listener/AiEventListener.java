package com.wreckloud.wolfchat.ai.application.listener;

import com.wreckloud.wolfchat.ai.application.client.AiTextClient;
import com.wreckloud.wolfchat.ai.application.service.AiIdentityService;
import com.wreckloud.wolfchat.ai.application.service.AiInteractionMemoryService;
import com.wreckloud.wolfchat.ai.application.service.AiMemoryDigestService;
import com.wreckloud.wolfchat.ai.application.service.AiMoodService;
import com.wreckloud.wolfchat.ai.application.service.AiPromptBuilderService;
import com.wreckloud.wolfchat.ai.application.service.AiRoleService;
import com.wreckloud.wolfchat.ai.application.service.AiSessionStateService;
import com.wreckloud.wolfchat.ai.application.service.AiSummaryService;
import com.wreckloud.wolfchat.ai.application.service.AiTaskSchedulerService;
import com.wreckloud.wolfchat.ai.application.service.AiUserMemoryService;
import com.wreckloud.wolfchat.ai.application.service.RecentMessageService;
import com.wreckloud.wolfchat.ai.application.support.AiInteractionContextSupport;
import com.wreckloud.wolfchat.ai.application.support.AiReplyComposeSupport;
import com.wreckloud.wolfchat.ai.application.support.AiResponseProcessor;
import com.wreckloud.wolfchat.ai.application.support.AiReplySplitSupport;
import com.wreckloud.wolfchat.ai.application.support.AiTimeRhythmSupport;
import com.wreckloud.wolfchat.ai.application.support.AiTriggerGuardSupport;
import com.wreckloud.wolfchat.ai.application.support.AiTriggerDecisionSupport;
import com.wreckloud.wolfchat.ai.config.AiConfig;
import com.wreckloud.wolfchat.chat.conversation.application.service.ConversationService;
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

import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Set;
import java.util.concurrent.ThreadLocalRandom;

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
    private static final int FORUM_AUTHOR_BURST_COOLDOWN_SECONDS = 12;
    private static final int FOLLOW_BACK_MIN_DELAY_SECONDS = 10;
    private static final int FOLLOW_BACK_MAX_DELAY_SECONDS = 20;
    private static final double FOLLOW_BACK_CHAT_FIRST_PROBABILITY = 0.68D;
    private static final int DEFAULT_PRIVATE_MAX_REPLY_CHARS = 180;
    private static final int DEFAULT_LOBBY_MAX_REPLY_CHARS = 140;
    private static final int DEFAULT_FORUM_MAX_REPLY_CHARS = 220;
    private static final double DEFAULT_MENTION_REPLY_PROBABILITY = 0.85D;
    private static final double DEFAULT_LOW_SIGNAL_PROBABILITY_MULTIPLIER = 0.35D;
    private static final double DEFAULT_OPEN_TOPIC_LOBBY_PROBABILITY = 0.34D;
    private static final double DEFAULT_OPEN_TOPIC_FORUM_THREAD_PROBABILITY = 0.42D;
    private static final double DEFAULT_OPEN_TOPIC_FORUM_REPLY_PROBABILITY = 0.30D;
    private static final int LOBBY_CONTEXT_SHIFTED_MESSAGE_THRESHOLD = 6;
    private static final double DEFAULT_RELEVANCE_THRESHOLD_GENERAL = 0.16D;
    private static final double DEFAULT_RELEVANCE_THRESHOLD_QUESTION = 0.24D;

    private static final String DEFAULT_PRIVATE_SYSTEM_PROMPT = "你是 WolfChat 的真人用户，请自然回复，短句优先。禁止自称AI。";
    private static final String DEFAULT_LOBBY_SYSTEM_PROMPT = "你在公共聊天室聊天，请像普通网友一样自然参与，不要刷屏。";
    private static final String DEFAULT_FORUM_SYSTEM_PROMPT = "你在社区讨论区回帖，请围绕主题给出简洁、有信息量的回复。";

    private final AiConfig aiConfig;
    private final AiTextClient aiTextClient;
    private final AiIdentityService aiIdentityService;
    private final AiRoleService aiRoleService;
    private final AiMoodService aiMoodService;
    private final AiMemoryDigestService aiMemoryDigestService;
    private final AiInteractionMemoryService aiInteractionMemoryService;
    private final RecentMessageService recentMessageService;
    private final AiSessionStateService aiSessionStateService;
    private final AiUserMemoryService aiUserMemoryService;
    private final AiSummaryService aiSummaryService;
    private final AiTaskSchedulerService aiTaskSchedulerService;
    private final AiPromptBuilderService aiPromptBuilderService;
    private final AiInteractionContextSupport aiInteractionContextSupport;
    private final AiReplyComposeSupport aiReplyComposeSupport;
    private final AiResponseProcessor aiResponseProcessor;
    private final AiReplySplitSupport aiReplySplitSupport;
    private final AiTimeRhythmSupport aiTimeRhythmSupport;
    private final AiTriggerGuardSupport aiTriggerGuardSupport;
    private final AiTriggerDecisionSupport aiTriggerDecisionSupport;
    private final ConversationService conversationService;
    private final MessageService messageService;
    private final LobbyService lobbyService;
    private final ForumService forumService;
    private final FollowService followService;
    private final ChatMessagePushService chatMessagePushService;
    private final UserPresenceService userPresenceService;
    private final WfLobbyMessageMapper wfLobbyMessageMapper;
    private final WfForumThreadMapper wfForumThreadMapper;
    private final WfForumReplyMapper wfForumReplyMapper;

    @EventListener
    public void onPrivateMessageSent(PrivateMessageSentEvent event) {
        if (event == null || !isRuntimeReady()) {
            return;
        }
        recentMessageService.invalidatePrivateConversationCache(event.getConversationId());
        AiConfig.PrivateChat privateChatConfig = aiConfig.getPrivateChat();
        if (privateChatConfig == null || !Boolean.TRUE.equals(privateChatConfig.getEnabled())) {
            return;
        }
        Long botUserId = aiIdentityService.getPrivateBotUserId();
        if (!isValidUserId(botUserId) || !botUserId.equals(event.getReceiverId())) {
            return;
        }
        if (isBlockedMessageTrigger(event.getSenderId(), event.getMsgType())) {
            return;
        }
        aiInteractionMemoryService.recordInteraction(PRIVATE_SCENE, botUserId, event.getSenderId(), event.getContent());
        boolean questionMessage = looksLikePrivateQuestion(event.getContent());
        String dedupKey = questionMessage
                ? PRIVATE_SCENE + ":" + event.getConversationId() + ":q:" + event.getMessageId()
                : PRIVATE_SCENE + ":" + event.getConversationId();
        if (!passPrivateTriggerGuard(event, botUserId, privateChatConfig, dedupKey)) {
            return;
        }

        scheduleTriggeredTask(
                dedupKey,
                privateChatConfig.getMinDelaySeconds(),
                privateChatConfig.getMaxDelaySeconds(),
                () -> processPrivateReply(event, botUserId, privateChatConfig),
                "AI 私聊"
        );
    }

    @EventListener
    public void onLobbyMessageSent(LobbyMessageSentEvent event) {
        if (event == null || !isRuntimeReady()) {
            return;
        }
        recentMessageService.invalidateLobbyCache();
        AiConfig.Lobby lobbyConfig = aiConfig.getLobby();
        if (lobbyConfig == null || !Boolean.TRUE.equals(lobbyConfig.getEnabled())) {
            return;
        }
        Long botUserId = aiIdentityService.getLobbyBotUserId();
        if (!isValidUserId(botUserId) || botUserId.equals(event.getSenderId())) {
            return;
        }
        if (isBlockedMessageTrigger(event.getSenderId(), event.getMsgType())) {
            return;
        }
        WfLobbyMessage triggerMessage = wfLobbyMessageMapper.selectById(event.getMessageId());
        boolean directedToBot = aiTriggerDecisionSupport.isDirectedLobbyMessage(
                event,
                triggerMessage,
                botUserId,
                aiInteractionContextSupport::resolveUserDisplayName
        );
        aiInteractionMemoryService.recordInteraction(LOBBY_SCENE, botUserId, event.getSenderId(), event.getContent());
        String dedupKey = directedToBot
                ? LOBBY_MENTION_SCENE + ":" + botUserId + ":" + event.getSenderId()
                : LOBBY_SCENE + ":" + botUserId + ":" + event.getSenderId();
        if (aiTaskSchedulerService.isPending(dedupKey)) {
            log.debug("AI 大厅触发跳过: reason=pending_task, dedupKey={}, directed={}", dedupKey, directedToBot);
            return;
        }

        DelayWindow delayWindow = resolveDelayWindow(
                directedToBot,
                lobbyConfig.getMentionMinDelaySeconds(),
                lobbyConfig.getMentionMaxDelaySeconds(),
                lobbyConfig.getMinDelaySeconds(),
                lobbyConfig.getMaxDelaySeconds());
        log.debug("AI 大厅调度窗口: directed={}, period={}, delay={}~{}s, senderId={}, messageId={}",
                directedToBot,
                aiTimeRhythmSupport.currentPeriodLabel(),
                delayWindow.minDelaySeconds,
                delayWindow.maxDelaySeconds,
                event.getSenderId(),
                event.getMessageId());
        scheduleTriggeredTask(
                dedupKey,
                delayWindow.minDelaySeconds,
                delayWindow.maxDelaySeconds,
                () -> processLobbyReply(event, botUserId, lobbyConfig),
                "AI 大厅"
        );
    }

    @EventListener
    public void onForumThreadCreated(ForumThreadCreatedEvent event) {
        if (event == null || !isRuntimeReady()) {
            return;
        }
        recentMessageService.invalidateForumThreadCache(event.getThreadId());
        AiConfig.Forum forumConfig = aiConfig.getForum();
        if (forumConfig == null || !Boolean.TRUE.equals(forumConfig.getEnabled())) {
            return;
        }
        Long botUserId = aiIdentityService.getForumBotUserId();
        if (!isValidUserId(botUserId) || botUserId.equals(event.getAuthorId())) {
            log.debug("AI 论坛主题触发跳过: reason=invalid_or_self, botUserId={}, authorId={}", botUserId, event.getAuthorId());
            return;
        }
        if (aiIdentityService.isAiUser(event.getAuthorId())) {
            log.debug("AI 论坛主题触发跳过: reason=author_is_ai, authorId={}", event.getAuthorId());
            return;
        }
        boolean directedToBot = aiTriggerDecisionSupport.containsAiMention(botUserId, event.getTitle(), event.getContent());
        aiInteractionMemoryService.recordInteraction(
                FORUM_SCENE,
                botUserId,
                event.getAuthorId(),
                buildForumInteractionText(event.getTitle(), event.getContent())
        );
        String dedupKey = directedToBot
                ? FORUM_MENTION_SCENE + ":thread:" + event.getThreadId()
                : FORUM_SCENE + ":thread:" + event.getThreadId();
        String triggerContent = buildForumInteractionText(event.getTitle(), event.getContent());
        if (!passForumTriggerGuard(
                botUserId,
                directedToBot,
                dedupKey,
                forumConfig.getMentionReplyProbability(),
                forumConfig.getReplyProbability(),
                0.36D,
                triggerContent,
                DEFAULT_OPEN_TOPIC_FORUM_THREAD_PROBABILITY,
                forumConfig.getCooldownSeconds(),
                String.valueOf(event.getThreadId()),
                forumConfig.getMaxRepliesPerHour(),
                forumConfig.getMaxRepliesPerDay()
        )) {
            return;
        }

        DelayWindow delayWindow = resolveDelayWindow(
                directedToBot,
                forumConfig.getMentionMinDelaySeconds(),
                forumConfig.getMentionMaxDelaySeconds(),
                forumConfig.getMinDelaySeconds(),
                forumConfig.getMaxDelaySeconds()
        );
        scheduleTriggeredTask(
                dedupKey,
                delayWindow.minDelaySeconds,
                delayWindow.maxDelaySeconds,
                () -> processForumReply(event.getThreadId(), null, botUserId, forumConfig, directedToBot),
                "AI 论坛主题"
        );
    }

    @EventListener
    public void onForumReplyCreated(ForumReplyCreatedEvent event) {
        if (event == null || !isRuntimeReady()) {
            return;
        }
        recentMessageService.invalidateForumThreadCache(event.getThreadId());
        AiConfig.Forum forumConfig = aiConfig.getForum();
        if (forumConfig == null || !Boolean.TRUE.equals(forumConfig.getEnabled())) {
            return;
        }
        Long botUserId = aiIdentityService.getForumBotUserId();
        if (!isValidUserId(botUserId) || botUserId.equals(event.getAuthorId())) {
            log.debug("AI 论坛回帖触发跳过: reason=invalid_or_self, botUserId={}, authorId={}", botUserId, event.getAuthorId());
            return;
        }
        if (aiIdentityService.isAiUser(event.getAuthorId())) {
            log.debug("AI 论坛回帖触发跳过: reason=author_is_ai, authorId={}", event.getAuthorId());
            return;
        }
        WfForumReply quoteReply = event.getQuoteReplyId() == null ? null : wfForumReplyMapper.selectById(event.getQuoteReplyId());
        boolean directedToBot = aiTriggerDecisionSupport.isDirectedForumReply(event, quoteReply, botUserId);
        if (!directedToBot && !allowForumAuthorBurst(event, botUserId)) {
            return;
        }
        aiInteractionMemoryService.recordInteraction(FORUM_SCENE, botUserId, event.getAuthorId(), event.getContent());
        String dedupKey = directedToBot
                ? FORUM_MENTION_SCENE + ":reply:" + event.getThreadId() + ":" + event.getAuthorId()
                : FORUM_SCENE + ":reply:" + event.getThreadId() + ":" + event.getAuthorId();
        if (!passForumTriggerGuard(
                botUserId,
                directedToBot,
                dedupKey,
                forumConfig.getMentionReplyProbability(),
                forumConfig.getReplyToReplyProbability(),
                0.24D,
                event.getContent(),
                DEFAULT_OPEN_TOPIC_FORUM_REPLY_PROBABILITY,
                forumConfig.getCooldownSeconds(),
                String.valueOf(event.getThreadId()),
                forumConfig.getMaxRepliesPerHour(),
                forumConfig.getMaxRepliesPerDay()
        )) {
            return;
        }

        DelayWindow delayWindow = resolveDelayWindow(
                directedToBot,
                forumConfig.getMentionMinDelaySeconds(),
                forumConfig.getMentionMaxDelaySeconds(),
                forumConfig.getMinDelaySeconds(),
                forumConfig.getMaxDelaySeconds()
        );
        scheduleTriggeredTask(
                dedupKey,
                delayWindow.minDelaySeconds,
                delayWindow.maxDelaySeconds,
                () -> processForumReply(event.getThreadId(), event.getReplyId(), botUserId, forumConfig, directedToBot),
                "AI 论坛回帖"
        );
    }

    @EventListener
    public void onUserFollowed(UserFollowedEvent event) {
        if (event == null || !aiIdentityService.isAiEnabled()) {
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
        int minDelaySeconds = resolveFollowBackMinDelaySeconds(followConfig.getMinDelaySeconds());
        int maxDelaySeconds = resolveFollowBackMaxDelaySeconds(followConfig.getMaxDelaySeconds(), minDelaySeconds);
        aiTaskSchedulerService.schedule(
                dedupKey,
                minDelaySeconds,
                maxDelaySeconds,
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
            memoryDigest = aiInteractionContextSupport.appendInteractionMemoryDigest(memoryDigest, PRIVATE_SCENE, botUserId);
            String privateSessionKey = String.valueOf(event.getConversationId());
            String engagementMode = aiSessionStateService.resolvePrivateEngagementMode(PRIVATE_SCENE, privateSessionKey, event.getContent());
            boolean conversationStalled = aiSessionStateService.isPrivateConversationStalled(PRIVATE_SCENE, privateSessionKey, event.getContent());
            memoryDigest = appendSummaryDigest(memoryDigest, aiSummaryService.getSummaryDigest(PRIVATE_SCENE, privateSessionKey));
            memoryDigest = appendSummaryDigest(memoryDigest, aiSessionStateService.buildStateDigest(PRIVATE_SCENE, privateSessionKey));
            memoryDigest = appendSummaryDigest(memoryDigest, aiUserMemoryService.buildMemoryDigest(botUserId, event.getSenderId(), 3));
            String prompt = aiPromptBuilderService.buildPrivatePrompt(
                    botUserId, event.getSenderId(), recentMessages, rolePrompt, moodDirective, memoryDigest, engagementMode, conversationStalled
            );
            String systemPrompt = normalizeSystemPrompt(privateChatConfig.getSystemPrompt(), DEFAULT_PRIVATE_SYSTEM_PROMPT);
            systemPrompt = appendRoleSystemPrompt(systemPrompt, roleProfile);
            log.debug("AI 私聊推理: conversationId={}, contextSize={}, promptChars={}, mode={}, stalled={}",
                    event.getConversationId(), recentMessages.size(), prompt.length(), engagementMode, conversationStalled);
            String rawReply = aiTextClient.complete(systemPrompt, prompt);
            rawReply = retryReplyWhenOffTopic(
                    rawReply,
                    systemPrompt,
                    prompt,
                    event.getContent(),
                    "私聊",
                    "conversationId=" + event.getConversationId()
            );
            String reply = aiResponseProcessor.processPrivateReply(
                    rawReply,
                    privateChatConfig.getMaxReplyChars(),
                    DEFAULT_PRIVATE_MAX_REPLY_CHARS,
                    event.getContent(),
                    engagementMode,
                    conversationStalled
            );
            if (!StringUtils.hasText(reply)) {
                return;
            }
            userPresenceService.markOnline(botUserId);
            List<String> segments = aiReplySplitSupport.splitPrivateReply(reply);
            if (segments.isEmpty()) {
                segments = List.of(reply);
            }
            Long lastMessageId = null;
            for (String segment : segments) {
                SendMessageCommand command = new SendMessageCommand();
                command.setUserId(botUserId);
                command.setConversationId(event.getConversationId());
                command.setMsgType(MessageType.TEXT);
                command.setContent(segment);
                WfMessage message = messageService.sendMessage(command);
                chatMessagePushService.pushPrivateMessageToReceiver(message);
                lastMessageId = message.getId();
            }
            scheduleMemoryArrange(
                    PRIVATE_SCENE,
                    privateSessionKey,
                    botUserId,
                    event.getSenderId(),
                    event.getContent(),
                    reply,
                    recentMessages.stream().map(WfMessage::getContent).toList()
            );
            log.info("AI 私聊回复完成: conversationId={}, messageId={}, splitCount={}, totalChars={}, role={}",
                    event.getConversationId(), lastMessageId, segments.size(), reply.length(), roleCode(roleProfile));
        } catch (Exception ex) {
            log.warn("AI 私聊回复失败: conversationId={}, message={}", event.getConversationId(), ex.getMessage());
        }
    }

    private void processLobbyReply(LobbyMessageSentEvent event,
                                   Long botUserId,
                                   AiConfig.Lobby lobbyConfig) {
        try {
            WfLobbyMessage triggerMessage = wfLobbyMessageMapper.selectById(event.getMessageId());
            boolean directedToBot = aiTriggerDecisionSupport.isDirectedLobbyMessage(
                    event,
                    triggerMessage,
                    botUserId,
                    aiInteractionContextSupport::resolveUserDisplayName
            );
            List<WfLobbyMessage> recentMessages = loadRecentLobbyMessages();
            if (!allowLobbyTriggerAtExecution(event, botUserId, lobbyConfig, directedToBot, recentMessages)) {
                return;
            }
            AiRoleService.AiRoleProfile roleProfile = aiRoleService.pickRole(LOBBY_SCENE);
            String rolePrompt = buildRolePrompt(roleProfile);
            String moodDirective = aiMoodService.buildMoodDirective(LOBBY_SCENE, botUserId, event.getContent());
            String memoryDigest = aiMemoryDigestService.buildLobbyDigest(recentMessages);
            memoryDigest = aiInteractionContextSupport.appendInteractionMemoryDigest(memoryDigest, LOBBY_SCENE, botUserId);
            memoryDigest = appendSummaryDigest(memoryDigest, aiSummaryService.getSummaryDigest(LOBBY_SCENE, "global"));
            memoryDigest = appendSummaryDigest(memoryDigest, aiSessionStateService.buildStateDigest(LOBBY_SCENE, "global"));
            memoryDigest = appendSummaryDigest(memoryDigest, aiUserMemoryService.buildMemoryDigest(botUserId, event.getSenderId(), 3));
            String prompt = aiPromptBuilderService.buildLobbyPrompt(
                    botUserId, event.getSenderId(), recentMessages, rolePrompt, moodDirective, memoryDigest
            );
            prompt = aiReplyComposeSupport.appendLobbyDirectedPrompt(
                    prompt,
                    directedToBot,
                    event.getSenderId(),
                    aiInteractionContextSupport::resolveUserDisplayName
            );
            String systemPrompt = normalizeSystemPrompt(lobbyConfig.getSystemPrompt(), DEFAULT_LOBBY_SYSTEM_PROMPT);
            systemPrompt = appendRoleSystemPrompt(systemPrompt, roleProfile);
            log.debug("AI 大厅推理: triggerMessageId={}, contextSize={}, promptChars={}",
                    event.getMessageId(), recentMessages.size(), prompt.length());
            String rawReply = aiTextClient.complete(systemPrompt, prompt);
            rawReply = retryReplyWhenOffTopic(
                    rawReply,
                    systemPrompt,
                    prompt,
                    event.getContent(),
                    "大厅",
                    "triggerMessageId=" + event.getMessageId()
            );
            String reply = aiResponseProcessor.processLobbyReply(
                    rawReply,
                    lobbyConfig.getMaxReplyChars(),
                    DEFAULT_LOBBY_MAX_REPLY_CHARS
            );
            if (!StringUtils.hasText(reply)) {
                return;
            }
            reply = aiReplyComposeSupport.normalizeLobbyDirectedReply(
                    reply,
                    directedToBot,
                    event.getSenderId(),
                    aiInteractionContextSupport::resolveUserDisplayName
            );
            List<String> segments = aiReplySplitSupport.splitLobbyReply(reply);
            if (segments.isEmpty()) {
                segments = List.of(reply);
            }
            userPresenceService.markOnline(botUserId);
            Long replyToMessageId = resolveLobbyReplyToMessageId(directedToBot, triggerMessage, recentMessages, botUserId);
            for (int i = 0; i < segments.size(); i++) {
                SendLobbyMessageCommand command = new SendLobbyMessageCommand();
                command.setUserId(botUserId);
                command.setMsgType(MessageType.TEXT);
                command.setContent(segments.get(i));
                if (i == 0 && replyToMessageId != null) {
                    command.setReplyToMessageId(replyToMessageId);
                }
                chatMessagePushService.pushLobbyMessage(botUserId, lobbyService.sendMessage(command));
            }
            scheduleMemoryArrange(
                    LOBBY_SCENE,
                    "global",
                    botUserId,
                    event.getSenderId(),
                    event.getContent(),
                    reply,
                    recentMessages.stream().map(WfLobbyMessage::getContent).toList()
            );
            log.info("AI 大厅回复完成: triggerMessageId={}, splitCount={}, totalChars={}, role={}",
                    event.getMessageId(), segments.size(), reply.length(), roleCode(roleProfile));
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
            memoryDigest = aiInteractionContextSupport.appendInteractionMemoryDigest(memoryDigest, FORUM_SCENE, botUserId);
            String forumSessionKey = String.valueOf(threadId);
            memoryDigest = appendSummaryDigest(memoryDigest, aiSummaryService.getSummaryDigest(FORUM_SCENE, forumSessionKey));
            memoryDigest = appendSummaryDigest(memoryDigest, aiSessionStateService.buildStateDigest(FORUM_SCENE, forumSessionKey));
            Long directedUserId = resolveForumDirectedUserId(thread, triggerReply);
            memoryDigest = appendSummaryDigest(memoryDigest, aiUserMemoryService.buildMemoryDigest(botUserId, directedUserId, 3));
            String prompt = aiPromptBuilderService.buildForumReplyPrompt(
                    botUserId, thread, triggerReply, recentReplies, rolePrompt, moodDirective, memoryDigest
            );
            prompt = aiReplyComposeSupport.appendForumDirectedPrompt(
                    prompt,
                    directedToBot,
                    directedUserId,
                    aiInteractionContextSupport::resolveUserDisplayName
            );
            String systemPrompt = normalizeSystemPrompt(forumConfig.getSystemPrompt(), DEFAULT_FORUM_SYSTEM_PROMPT);
            systemPrompt = appendRoleSystemPrompt(systemPrompt, roleProfile);
            log.debug("AI 论坛推理: threadId={}, quoteReplyId={}, contextSize={}, promptChars={}",
                    threadId, quoteReplyId, recentReplies.size(), prompt.length());
            String reply = aiResponseProcessor.processForumReply(
                    aiTextClient.complete(systemPrompt, prompt),
                    forumConfig.getMaxReplyChars(),
                    DEFAULT_FORUM_MAX_REPLY_CHARS
            );
            if (!StringUtils.hasText(reply)) {
                return;
            }
            reply = aiReplyComposeSupport.normalizeForumDirectedReply(
                    reply,
                    directedToBot,
                    directedUserId,
                    aiInteractionContextSupport::resolveUserDisplayName
            );

            CreateReplyDTO dto = new CreateReplyDTO();
            dto.setContent(reply);
            if (quoteReplyId != null && quoteReplyId > 0L && triggerReply != null && ForumReplyStatus.NORMAL.equals(triggerReply.getStatus())) {
                dto.setQuoteReplyId(quoteReplyId);
            }
            userPresenceService.markActive(botUserId);
            forumService.createReply(botUserId, threadId, dto);
            scheduleMemoryArrange(
                    FORUM_SCENE,
                    forumSessionKey,
                    botUserId,
                    directedUserId,
                    triggerText,
                    reply,
                    recentReplies.stream().map(WfForumReply::getContent).toList()
            );
            log.info("AI 论坛回复完成: threadId={}, quoteReplyId={}, role={}",
                    threadId, quoteReplyId, roleCode(roleProfile));
        } catch (Exception ex) {
            log.warn("AI 论坛回复失败: threadId={}, message={}", threadId, ex.getMessage());
        }
    }

    private void processAutoFollowBack(Long botUserId, Long targetUserId) {
        try {
            if (followService.isMutualFollow(botUserId, targetUserId)) {
                return;
            }
            userPresenceService.markActive(botUserId);
            if (shouldSendFollowBackWarmupMessage()) {
                sendFollowBackWarmupMessages(botUserId, targetUserId);
            }
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

    private int resolveFollowBackMinDelaySeconds(Integer configuredMinDelaySeconds) {
        int configured = configuredMinDelaySeconds == null ? FOLLOW_BACK_MIN_DELAY_SECONDS : configuredMinDelaySeconds;
        return Math.max(FOLLOW_BACK_MIN_DELAY_SECONDS, configured);
    }

    private int resolveFollowBackMaxDelaySeconds(Integer configuredMaxDelaySeconds, int minDelaySeconds) {
        int configured = configuredMaxDelaySeconds == null ? FOLLOW_BACK_MAX_DELAY_SECONDS : configuredMaxDelaySeconds;
        return Math.max(minDelaySeconds, Math.max(FOLLOW_BACK_MAX_DELAY_SECONDS, configured));
    }

    private boolean shouldSendFollowBackWarmupMessage() {
        return ThreadLocalRandom.current().nextDouble() < FOLLOW_BACK_CHAT_FIRST_PROBABILITY;
    }

    private void sendFollowBackWarmupMessages(Long botUserId, Long targetUserId) {
        try {
            Long conversationId = conversationService.getOrCreateConversation(botUserId, targetUserId);
            int lineCount = ThreadLocalRandom.current().nextInt(1, 3);
            List<String> candidates = List.of(
                    "行，关注我了是吧，先打个招呼。",
                    "你来得挺快，最近在忙啥？",
                    "我先跟你聊两句，别把我当自动机。",
                    "刚看到你关注，想聊什么直接说。"
            );
            Set<Integer> pickedIndexes = new HashSet<>();
            for (int i = 0; i < lineCount; i++) {
                int index = pickWarmupMessageIndex(candidates.size(), pickedIndexes);
                if (index < 0) {
                    continue;
                }
                SendMessageCommand command = new SendMessageCommand();
                command.setUserId(botUserId);
                command.setConversationId(conversationId);
                command.setMsgType(MessageType.TEXT);
                command.setContent(candidates.get(index));
                WfMessage message = messageService.sendMessage(command);
                chatMessagePushService.pushPrivateMessageToReceiver(message);
            }
        } catch (Exception ex) {
            // 暖场失败不影响回关主链路。
            log.debug("AI 回关前私聊铺垫失败: botUserId={}, targetUserId={}, message={}",
                    botUserId, targetUserId, ex.getMessage());
        }
    }

    private int pickWarmupMessageIndex(int candidateSize, Set<Integer> pickedIndexes) {
        if (candidateSize <= 0 || pickedIndexes == null) {
            return -1;
        }
        if (pickedIndexes.size() >= candidateSize) {
            return -1;
        }
        int index = ThreadLocalRandom.current().nextInt(candidateSize);
        int loopGuard = 0;
        while (pickedIndexes.contains(index) && loopGuard < candidateSize * 2) {
            index = ThreadLocalRandom.current().nextInt(candidateSize);
            loopGuard++;
        }
        if (pickedIndexes.contains(index)) {
            for (int i = 0; i < candidateSize; i++) {
                if (!pickedIndexes.contains(i)) {
                    index = i;
                    break;
                }
            }
        }
        pickedIndexes.add(index);
        return index;
    }

    private boolean isValidUserId(Long userId) {
        return userId != null && userId > 0L;
    }

    private boolean isBlockedMessageTrigger(Long senderId, MessageType msgType) {
        return aiIdentityService.isAiUser(senderId) || isUnsupportedTriggerType(msgType);
    }

    private boolean allowForumAuthorBurst(ForumReplyCreatedEvent event, Long botUserId) {
        if (event == null || !isValidUserId(botUserId) || !isValidUserId(event.getAuthorId())) {
            return true;
        }
        if (event.getThreadId() == null || event.getThreadId() <= 0L) {
            return true;
        }
        String target = event.getThreadId() + ":" + event.getAuthorId();
        return aiTriggerGuardSupport.allowByCooldown(
                FORUM_SCENE + ":author-burst",
                botUserId,
                target,
                FORUM_AUTHOR_BURST_COOLDOWN_SECONDS
        );
    }

    private boolean passPrivateTriggerGuard(PrivateMessageSentEvent event,
                                            Long botUserId,
                                            AiConfig.PrivateChat privateChatConfig,
                                            String dedupKey) {
        if (aiTaskSchedulerService.isPending(dedupKey)) {
            return false;
        }
        Double configuredProbability = privateChatConfig.getReplyProbability();
        double baseProbability = configuredProbability == null ? 0.85D : configuredProbability;
        boolean questionMessage = looksLikePrivateQuestion(event.getContent());
        if (questionMessage) {
            // 私聊问题优先接住，尽量减少“问了不回”的体感。
            baseProbability = 1D;
        }
        baseProbability = Math.max(baseProbability, 0.9D);
        if (looksLikePrivateBanter(event.getContent())) {
            baseProbability = Math.max(baseProbability, 0.9D);
        }
        if (looksLikePrivateGreeting(event.getContent())) {
            baseProbability = Math.max(baseProbability, 0.96D);
        }
        double rhythmProbability = aiTimeRhythmSupport.adjustProbability(baseProbability);
        if (!aiTriggerGuardSupport.hitProbability(rhythmProbability, rhythmProbability)) {
            log.debug("AI 私聊触发跳过: reason=probability, conversationId={}, probability={}",
                    event.getConversationId(), rhythmProbability);
            return false;
        }
        int cooldownSeconds = aiTimeRhythmSupport.adjustCooldown(privateChatConfig.getCooldownSeconds(), 22);
        cooldownSeconds = Math.max(4, (int) Math.round(cooldownSeconds * 0.85D));
        if (questionMessage) {
            cooldownSeconds = Math.max(2, cooldownSeconds / 3);
        }
        if (looksLikePrivateBanter(event.getContent())) {
            cooldownSeconds = Math.max(8, (int) Math.round(cooldownSeconds * 0.75D));
        }
        if (looksLikePrivateGreeting(event.getContent())) {
            cooldownSeconds = Math.max(6, cooldownSeconds / 2);
        }
        if (!aiTriggerGuardSupport.allowByCooldown(
                PRIVATE_SCENE,
                botUserId,
                String.valueOf(event.getConversationId()),
                cooldownSeconds
        )) {
            log.debug("AI 私聊触发跳过: reason=cooldown, conversationId={}, cooldownSeconds={}",
                    event.getConversationId(), cooldownSeconds);
            return false;
        }
        if (!aiTriggerGuardSupport.allowByHourlyLimit(PRIVATE_SCENE, botUserId, privateChatConfig.getMaxRepliesPerHour())) {
            log.debug("AI 私聊触发跳过: reason=hourly_limit, botUserId={}", botUserId);
            return false;
        }
        return aiTriggerGuardSupport.allowGlobalQuota(PRIVATE_SCENE, botUserId);
    }

    private boolean looksLikePrivateGreeting(String content) {
        if (!StringUtils.hasText(content)) {
            return false;
        }
        String lower = content.trim().toLowerCase();
        return lower.contains("你好")
                || lower.contains("哈喽")
                || lower.contains("嗨")
                || lower.contains("在吗")
                || lower.contains("有人吗")
                || lower.contains("在不在")
                || lower.contains("还在吗")
                || "在".equals(lower)
                || "在吗".equals(lower);
    }

    private boolean looksLikePrivateBanter(String content) {
        if (!StringUtils.hasText(content)) {
            return false;
        }
        String lower = content.trim().toLowerCase();
        return lower.contains("哈哈")
                || lower.contains("笑死")
                || lower.contains("离谱")
                || lower.contains("抽象")
                || lower.contains("梗")
                || lower.contains("6")
                || lower.contains("牛");
    }

    private boolean looksLikePrivateQuestion(String content) {
        if (!StringUtils.hasText(content)) {
            return false;
        }
        String text = content.trim();
        if (!StringUtils.hasText(text)) {
            return false;
        }
        String lower = text.toLowerCase();
        return lower.contains("?")
                || lower.contains("？")
                || lower.contains("吗")
                || lower.contains("咋")
                || lower.contains("怎么")
                || lower.contains("如何")
                || lower.contains("为什么")
                || lower.contains("为啥")
                || lower.contains("能不能")
                || lower.contains("可不可以")
                || lower.contains("是不是")
                || lower.contains("行不行")
                || lower.startsWith("那")
                || lower.startsWith("所以");
    }

    private boolean looksLikeOpenTopicText(String content) {
        if (!StringUtils.hasText(content)) {
            return false;
        }
        String lower = content.trim().toLowerCase(Locale.ROOT);
        if (!StringUtils.hasText(lower) || lower.length() < 4) {
            return false;
        }
        return lower.contains("?")
                || lower.contains("？")
                || lower.contains("大家")
                || lower.contains("有人")
                || lower.contains("你们觉得")
                || lower.contains("怎么看")
                || lower.contains("求助")
                || lower.contains("推荐")
                || lower.contains("有没有")
                || lower.contains("怎么")
                || lower.contains("为什么")
                || lower.contains("为啥")
                || lower.contains("聊聊")
                || lower.contains("讨论");
    }

    private boolean passLobbyTriggerGuard(LobbyMessageSentEvent event,
                                          Long botUserId,
                                          AiConfig.Lobby lobbyConfig,
                                          boolean directedToBot) {
        double defaultProbability = directedToBot ? DEFAULT_MENTION_REPLY_PROBABILITY : 0.25D;
        Double configuredProbability = directedToBot
                ? lobbyConfig.getMentionReplyProbability()
                : lobbyConfig.getReplyProbability();
        double baseProbability = configuredProbability == null ? defaultProbability : configuredProbability;
        if (directedToBot) {
            // 点名/追问场景尽量接住，减少“问了不回”。
            baseProbability = 1D;
        }
        if (!directedToBot
                && allowLowSignalGuard(lobbyConfig)
                && aiTriggerDecisionSupport.isLowSignalLobbyMessage(event.getContent())) {
            double multiplier = resolveLowSignalProbabilityMultiplier(lobbyConfig);
            baseProbability = baseProbability * multiplier;
            log.debug("AI 大厅低信息短句降触发: senderId={}, multiplier={}, adjustedProbability={}",
                    event.getSenderId(), multiplier, baseProbability);
        }
        if (!directedToBot && looksLikeOpenTopicText(event.getContent())) {
            baseProbability = Math.max(baseProbability, DEFAULT_OPEN_TOPIC_LOBBY_PROBABILITY);
            log.debug("AI 大厅开放话题加权: senderId={}, adjustedProbability={}",
                    event.getSenderId(), baseProbability);
        }
        double rhythmProbability = aiTimeRhythmSupport.adjustProbability(baseProbability);
        if (!aiTriggerGuardSupport.hitProbability(rhythmProbability, rhythmProbability)) {
            log.debug("AI 大厅触发跳过: reason=probability, directed={}, probability={}, senderId={}",
                    directedToBot, rhythmProbability, event.getSenderId());
            return false;
        }
        String cooldownScene = directedToBot ? LOBBY_MENTION_SCENE : LOBBY_SCENE;
        String cooldownTarget = directedToBot ? String.valueOf(event.getSenderId()) : "global";
        int cooldownSeconds = aiTimeRhythmSupport.adjustCooldown(lobbyConfig.getCooldownSeconds(), 32);
        if (directedToBot) {
            cooldownSeconds = Math.max(3, Math.min(cooldownSeconds, 8));
        }
        if (!aiTriggerGuardSupport.allowByCooldown(cooldownScene, botUserId, cooldownTarget, cooldownSeconds)) {
            log.debug("AI 大厅触发跳过: reason=cooldown, directed={}, target={}, cooldownSeconds={}",
                    directedToBot, cooldownTarget, cooldownSeconds);
            return false;
        }
        if (!aiTriggerGuardSupport.allowByHourlyLimit(LOBBY_SCENE, botUserId, lobbyConfig.getMaxRepliesPerHour())) {
            log.debug("AI 大厅触发跳过: reason=hourly_limit, botUserId={}", botUserId);
            return false;
        }
        return aiTriggerGuardSupport.allowGlobalQuota(LOBBY_SCENE, botUserId);
    }

    private boolean passForumTriggerGuard(Long botUserId,
                                          boolean directedToBot,
                                          String dedupKey,
                                          Double mentionProbability,
                                          Double normalConfiguredProbability,
                                          double normalDefaultProbability,
                                          String triggerContent,
                                          double openTopicMinProbability,
                                          Integer cooldownSeconds,
                                          String cooldownTarget,
                                          Integer maxRepliesPerHour,
                                          Integer maxRepliesPerDay) {
        if (aiTaskSchedulerService.isPending(dedupKey)) {
            return false;
        }
        double defaultProbability = directedToBot ? DEFAULT_MENTION_REPLY_PROBABILITY : normalDefaultProbability;
        Double configuredProbability = directedToBot ? mentionProbability : normalConfiguredProbability;
        double baseProbability = configuredProbability == null ? defaultProbability : configuredProbability;
        if (directedToBot) {
            // 点名/直接回复场景尽量接住，减少“喊了不回”体感。
            baseProbability = 1D;
        }
        if (!directedToBot && looksLikeOpenTopicText(triggerContent)) {
            baseProbability = Math.max(baseProbability, openTopicMinProbability);
            log.debug("AI 论坛开放话题加权: target={}, adjustedProbability={}", cooldownTarget, baseProbability);
        }
        double rhythmProbability = aiTimeRhythmSupport.adjustProbability(baseProbability);
        if (!aiTriggerGuardSupport.hitProbability(rhythmProbability, rhythmProbability)) {
            log.debug("AI 论坛触发跳过: reason=probability, directed={}, probability={}", directedToBot, rhythmProbability);
            return false;
        }
        String cooldownScene = directedToBot ? FORUM_MENTION_SCENE : FORUM_SCENE;
        int rhythmCooldownSeconds = aiTimeRhythmSupport.adjustCooldown(cooldownSeconds, 30);
        if (!aiTriggerGuardSupport.allowByCooldown(cooldownScene, botUserId, cooldownTarget, rhythmCooldownSeconds)) {
            log.debug("AI 论坛触发跳过: reason=cooldown, directed={}, target={}, cooldownSeconds={}",
                    directedToBot, cooldownTarget, rhythmCooldownSeconds);
            return false;
        }
        if (!aiTriggerGuardSupport.allowByHourlyLimit(FORUM_SCENE, botUserId, maxRepliesPerHour)) {
            log.debug("AI 论坛触发跳过: reason=hourly_limit, botUserId={}", botUserId);
            return false;
        }
        if (!aiTriggerGuardSupport.allowByDailyLimit(FORUM_SCENE, botUserId, maxRepliesPerDay)) {
            log.debug("AI 论坛触发跳过: reason=daily_limit, botUserId={}", botUserId);
            return false;
        }
        return aiTriggerGuardSupport.allowGlobalQuota(FORUM_SCENE, botUserId);
    }

    private DelayWindow resolveDelayWindow(boolean directedToBot,
                                           Integer mentionMinDelaySeconds,
                                           Integer mentionMaxDelaySeconds,
                                           Integer defaultMinDelaySeconds,
                                           Integer defaultMaxDelaySeconds) {
        int minDelaySeconds;
        int maxDelaySeconds;
        if (!directedToBot) {
            minDelaySeconds = defaultMinDelaySeconds == null ? 2 : Math.max(1, defaultMinDelaySeconds);
            maxDelaySeconds = defaultMaxDelaySeconds == null ? minDelaySeconds : Math.max(minDelaySeconds, defaultMaxDelaySeconds);
        } else {
            minDelaySeconds = aiTriggerGuardSupport.resolveMentionMinDelaySeconds(mentionMinDelaySeconds, defaultMinDelaySeconds);
            maxDelaySeconds = aiTriggerGuardSupport.resolveMentionMaxDelaySeconds(
                    mentionMaxDelaySeconds,
                    defaultMaxDelaySeconds,
                    minDelaySeconds
            );
        }
        AiTimeRhythmSupport.DelayWindow window = aiTimeRhythmSupport.adjustDelayWindow(minDelaySeconds, maxDelaySeconds);
        return new DelayWindow(window.minDelaySeconds(), window.maxDelaySeconds());
    }

    private Long resolveLobbyReplyToMessageId(boolean directedToBot,
                                              WfLobbyMessage triggerMessage,
                                              List<WfLobbyMessage> recentMessages,
                                              Long botUserId) {
        if (!directedToBot || triggerMessage == null || triggerMessage.getId() == null) {
            return null;
        }
        if (recentMessages == null || recentMessages.isEmpty()) {
            return triggerMessage.getId();
        }
        int triggerIndex = findLobbyMessageIndexById(recentMessages, triggerMessage.getId());
        if (triggerIndex < 0) {
            return triggerMessage.getId();
        }
        int nonBotMessagesAfterTrigger = 0;
        WfLobbyMessage latestNonBotMessageAfterTrigger = null;
        for (int i = triggerIndex + 1; i < recentMessages.size(); i++) {
            WfLobbyMessage message = recentMessages.get(i);
            if (message == null || botUserId.equals(message.getSenderId())) {
                continue;
            }
            nonBotMessagesAfterTrigger++;
            latestNonBotMessageAfterTrigger = message;
        }
        if (nonBotMessagesAfterTrigger <= 1) {
            // 紧邻上一条时不挂回复引用，优先自然接话。
            return null;
        }
        if (nonBotMessagesAfterTrigger >= 3) {
            return triggerMessage.getId();
        }
        if (latestNonBotMessageAfterTrigger == null) {
            return null;
        }
        return isLobbyTopicShifted(triggerMessage.getContent(), latestNonBotMessageAfterTrigger.getContent())
                ? triggerMessage.getId()
                : null;
    }

    private int findLobbyMessageIndexById(List<WfLobbyMessage> recentMessages, Long messageId) {
        if (recentMessages == null || recentMessages.isEmpty() || messageId == null) {
            return -1;
        }
        for (int i = 0; i < recentMessages.size(); i++) {
            WfLobbyMessage message = recentMessages.get(i);
            if (message != null && messageId.equals(message.getId())) {
                return i;
            }
        }
        return -1;
    }

    private boolean isLobbyTopicShifted(String triggerContent, String latestContent) {
        String trigger = normalizeTopicText(triggerContent);
        String latest = normalizeTopicText(latestContent);
        if (!StringUtils.hasText(trigger) || !StringUtils.hasText(latest)) {
            return false;
        }
        if (trigger.equals(latest) || trigger.contains(latest) || latest.contains(trigger)) {
            return false;
        }
        if (trigger.length() < 6 || latest.length() < 6) {
            return false;
        }
        Set<Character> triggerChars = new HashSet<>();
        for (char ch : trigger.toCharArray()) {
            triggerChars.add(ch);
        }
        Set<Character> latestChars = new HashSet<>();
        for (char ch : latest.toCharArray()) {
            latestChars.add(ch);
        }
        int intersection = 0;
        for (Character ch : triggerChars) {
            if (latestChars.contains(ch)) {
                intersection++;
            }
        }
        int minBase = Math.max(1, Math.min(triggerChars.size(), latestChars.size()));
        double overlapRatio = (double) intersection / (double) minBase;
        return overlapRatio < 0.28D;
    }

    private String normalizeTopicText(String text) {
        if (!StringUtils.hasText(text)) {
            return null;
        }
        return text.replaceAll("@\\S+", "")
                .replaceAll("[\\p{Punct}\\p{IsPunctuation}\\s]+", "")
                .trim()
                .toLowerCase(Locale.ROOT);
    }

    private void scheduleTriggeredTask(String dedupKey,
                                       Integer minDelaySeconds,
                                       Integer maxDelaySeconds,
                                       Runnable task,
                                       String sceneLabel) {
        boolean scheduled = aiTaskSchedulerService.schedule(dedupKey, minDelaySeconds, maxDelaySeconds, task);
        if (!scheduled) {
            log.debug("{}触发跳过: dedupKey={}", sceneLabel, dedupKey);
        }
    }

    private boolean allowLobbyTriggerAtExecution(LobbyMessageSentEvent event,
                                                 Long botUserId,
                                                 AiConfig.Lobby lobbyConfig,
                                                 boolean directedToBot,
                                                 List<WfLobbyMessage> recentMessages) {
        if (!passLobbyTriggerGuard(event, botUserId, lobbyConfig, directedToBot)) {
            log.debug("AI 大厅执行时跳过: reason=trigger_guard, directed={}, messageId={}", directedToBot, event.getMessageId());
            return false;
        }
        if (recentMessages == null || recentMessages.isEmpty()) {
            return true;
        }
        WfLobbyMessage latest = recentMessages.get(recentMessages.size() - 1);
        if (latest != null && botUserId.equals(latest.getSenderId())) {
            log.debug("AI 大厅执行时跳过：最后一条消息已由机器人发送, triggerMessageId={}", event.getMessageId());
            return false;
        }
        if (directedToBot) {
            return true;
        }
        int messageAfterTrigger = 0;
        for (int i = recentMessages.size() - 1; i >= 0; i--) {
            WfLobbyMessage message = recentMessages.get(i);
            if (message == null) {
                continue;
            }
            if (message.getId() != null && event.getMessageId() != null && message.getId() <= event.getMessageId()) {
                break;
            }
            if (!botUserId.equals(message.getSenderId())) {
                messageAfterTrigger++;
            }
            if (messageAfterTrigger >= LOBBY_CONTEXT_SHIFTED_MESSAGE_THRESHOLD) {
                log.debug("AI 大厅执行时跳过: reason=context_shifted, messageId={}, shiftedMessages={}",
                        event.getMessageId(), messageAfterTrigger);
                return false;
            }
        }
        return true;
    }

    private String appendSummaryDigest(String baseDigest, String extraDigest) {
        if (!StringUtils.hasText(extraDigest)) {
            return baseDigest;
        }
        if (!StringUtils.hasText(baseDigest)) {
            return extraDigest.trim();
        }
        return baseDigest + "\n" + extraDigest.trim();
    }

    private String retryReplyWhenOffTopic(String rawReply,
                                          String systemPrompt,
                                          String prompt,
                                          String latestUserMessage,
                                          String sceneLabel,
                                          String traceTag) {
        RelevanceCheck firstCheck = evaluateRelevance(rawReply, latestUserMessage);
        if (!firstCheck.shouldRetry) {
            return rawReply;
        }
        log.debug("AI {}相关性偏低，触发纠偏: {}, score={}, threshold={}",
                sceneLabel, traceTag, firstCheck.overlapScore, firstCheck.threshold);
        try {
            String retryPrompt = prompt
                    + "\n纠偏要求：你上一版回复与最后一条用户消息关联度不足。"
                    + "请先直接回应最后一条消息的核心问题/观点，再补一句自然延展；禁止换题。";
            String retried = aiTextClient.complete(systemPrompt, retryPrompt);
            if (!StringUtils.hasText(retried)) {
                return rawReply;
            }
            RelevanceCheck secondCheck = evaluateRelevance(retried, latestUserMessage);
            log.debug("AI {}纠偏重试结果: {}, before={}, after={}, threshold={}",
                    sceneLabel, traceTag, firstCheck.overlapScore, secondCheck.overlapScore, secondCheck.threshold);
            if (!StringUtils.hasText(rawReply)) {
                return retried;
            }
            return secondCheck.overlapScore >= firstCheck.overlapScore ? retried : rawReply;
        } catch (Exception ex) {
            log.debug("AI {}纠偏重试失败: {}, message={}", sceneLabel, traceTag, ex.getMessage());
        }
        return rawReply;
    }

    private RelevanceCheck evaluateRelevance(String reply, String latestUserMessage) {
        if (!isRelevanceGuardEnabled()) {
            return new RelevanceCheck(false, 1D, 0D);
        }
        if (!StringUtils.hasText(latestUserMessage)) {
            return new RelevanceCheck(false, 1D, 0D);
        }
        boolean questionText = looksLikeQuestionText(latestUserMessage);
        double threshold = questionText
                ? resolveQuestionRelevanceThreshold()
                : resolveGeneralRelevanceThreshold();
        String target = normalizeRelevanceText(latestUserMessage);
        if (!StringUtils.hasText(target)) {
            return new RelevanceCheck(false, 1D, threshold);
        }
        if (target.length() <= 4) {
            threshold = Math.min(threshold, 0.1D);
        }
        if (!StringUtils.hasText(reply)) {
            return new RelevanceCheck(true, 0D, threshold);
        }
        String candidate = normalizeRelevanceText(reply);
        if (!StringUtils.hasText(candidate)) {
            return new RelevanceCheck(true, 0D, threshold);
        }
        double charOverlap = computeCharacterOverlap(target, candidate);
        double bigramOverlap = computeBigramOverlap(target, candidate);
        double score = Math.max(charOverlap, bigramOverlap);
        return new RelevanceCheck(score < threshold, score, threshold);
    }

    private double computeCharacterOverlap(String left, String right) {
        Set<Character> leftSet = new HashSet<>();
        Set<Character> rightSet = new HashSet<>();
        for (char ch : left.toCharArray()) {
            if (isRelevanceNoiseChar(ch)) {
                continue;
            }
            leftSet.add(ch);
        }
        for (char ch : right.toCharArray()) {
            if (isRelevanceNoiseChar(ch)) {
                continue;
            }
            rightSet.add(ch);
        }
        if (leftSet.isEmpty() || rightSet.isEmpty()) {
            return 0D;
        }
        int intersection = 0;
        for (Character ch : leftSet) {
            if (rightSet.contains(ch)) {
                intersection++;
            }
        }
        int base = Math.max(1, Math.min(leftSet.size(), rightSet.size()));
        return (double) intersection / (double) base;
    }

    private double computeBigramOverlap(String left, String right) {
        Set<String> leftSet = toBigrams(left);
        Set<String> rightSet = toBigrams(right);
        if (leftSet.isEmpty() || rightSet.isEmpty()) {
            return 0D;
        }
        int intersection = 0;
        for (String gram : leftSet) {
            if (rightSet.contains(gram)) {
                intersection++;
            }
        }
        int base = Math.max(1, Math.min(leftSet.size(), rightSet.size()));
        return (double) intersection / (double) base;
    }

    private Set<String> toBigrams(String text) {
        Set<String> grams = new HashSet<>();
        if (!StringUtils.hasText(text) || text.length() < 2) {
            return grams;
        }
        for (int i = 0; i < text.length() - 1; i++) {
            char a = text.charAt(i);
            char b = text.charAt(i + 1);
            if (isRelevanceNoiseChar(a) || isRelevanceNoiseChar(b)) {
                continue;
            }
            grams.add(new String(new char[]{a, b}));
        }
        return grams;
    }

    private boolean isRelevanceNoiseChar(char ch) {
        return ch == '的'
                || ch == '了'
                || ch == '吗'
                || ch == '呢'
                || ch == '啊'
                || ch == '吧'
                || ch == '呀'
                || ch == '这'
                || ch == '那'
                || Character.isWhitespace(ch);
    }

    private boolean looksLikeQuestionText(String text) {
        if (!StringUtils.hasText(text)) {
            return false;
        }
        String lower = text.trim().toLowerCase();
        return lower.contains("?")
                || lower.contains("？")
                || lower.contains("吗")
                || lower.contains("怎么")
                || lower.contains("为啥")
                || lower.contains("为什么")
                || lower.contains("是不是")
                || lower.contains("能不能");
    }

    private String normalizeRelevanceText(String text) {
        if (!StringUtils.hasText(text)) {
            return null;
        }
        return text.trim()
                .replaceAll("@\\S+", "")
                .replaceAll("[\\p{Punct}\\p{IsPunctuation}\\s]+", "")
                .toLowerCase(Locale.ROOT);
    }

    private boolean isRelevanceGuardEnabled() {
        AiConfig.Relevance relevance = aiConfig.getRelevance();
        if (relevance == null) {
            return true;
        }
        return !Boolean.FALSE.equals(relevance.getEnabled());
    }

    private double resolveGeneralRelevanceThreshold() {
        AiConfig.Relevance relevance = aiConfig.getRelevance();
        Double configured = relevance == null ? null : relevance.getRetryThresholdGeneral();
        if (configured == null) {
            return DEFAULT_RELEVANCE_THRESHOLD_GENERAL;
        }
        return clampRelevanceThreshold(configured);
    }

    private double resolveQuestionRelevanceThreshold() {
        AiConfig.Relevance relevance = aiConfig.getRelevance();
        Double configured = relevance == null ? null : relevance.getRetryThresholdQuestion();
        if (configured == null) {
            return DEFAULT_RELEVANCE_THRESHOLD_QUESTION;
        }
        return clampRelevanceThreshold(configured);
    }

    private double clampRelevanceThreshold(double threshold) {
        return Math.max(0.05D, Math.min(0.8D, threshold));
    }

    private static final class RelevanceCheck {
        private final boolean shouldRetry;
        private final double overlapScore;
        private final double threshold;

        private RelevanceCheck(boolean shouldRetry, double overlapScore, double threshold) {
            this.shouldRetry = shouldRetry;
            this.overlapScore = overlapScore;
            this.threshold = threshold;
        }
    }

    private boolean allowLowSignalGuard(AiConfig.Lobby lobbyConfig) {
        if (lobbyConfig == null) {
            return true;
        }
        return !Boolean.FALSE.equals(lobbyConfig.getLowSignalGuardEnabled());
    }

    private double resolveLowSignalProbabilityMultiplier(AiConfig.Lobby lobbyConfig) {
        Double configured = lobbyConfig == null ? null : lobbyConfig.getLowSignalProbabilityMultiplier();
        if (configured == null || configured <= 0D) {
            return DEFAULT_LOW_SIGNAL_PROBABILITY_MULTIPLIER;
        }
        return Math.max(0.1D, Math.min(configured, 1.0D));
    }

    private void scheduleMemoryArrange(String scene,
                                       String sessionKey,
                                       Long botUserId,
                                       Long userId,
                                       String lastUserMessage,
                                       String lastAiReply,
                                       List<String> recentLines) {
        if (!StringUtils.hasText(scene) || !StringUtils.hasText(sessionKey)) {
            return;
        }
        String dedupKey = "ai-memory:" + scene + ":" + sessionKey;
        aiTaskSchedulerService.schedule(dedupKey, 0, 0, () -> {
            aiSessionStateService.upsertState(scene, sessionKey, botUserId, userId, lastUserMessage, lastAiReply);
            aiUserMemoryService.updateFacts(botUserId, userId, scene, lastUserMessage, lastAiReply);
            aiSummaryService.refreshSummary(scene, sessionKey, botUserId, recentLines);
        });
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

    private List<WfMessage> loadRecentPrivateMessages(Long conversationId) {
        return recentMessageService.loadRecentPrivateMessages(conversationId, resolveContextLimit());
    }

    private List<WfLobbyMessage> loadRecentLobbyMessages() {
        return recentMessageService.loadRecentLobbyMessages(resolveContextLimit());
    }

    private List<WfForumReply> loadRecentForumReplies(Long threadId) {
        return recentMessageService.loadRecentForumReplies(threadId, resolveContextLimit());
    }

    private int resolveContextLimit() {
        Integer configured = aiConfig.getMemory() == null ? null : aiConfig.getMemory().getRecentWindowSize();
        if (configured == null || configured <= 0) {
            return DEFAULT_CONTEXT_LIMIT;
        }
        return Math.min(configured, MAX_CONTEXT_LIMIT);
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

    private Long resolveForumDirectedUserId(WfForumThread thread, WfForumReply triggerReply) {
        if (triggerReply != null) {
            return triggerReply.getAuthorId();
        }
        return thread == null ? null : thread.getAuthorId();
    }

    private boolean isVisibleThreadStatus(ForumThreadStatus status) {
        return ForumThreadStatus.NORMAL.equals(status) || ForumThreadStatus.LOCKED.equals(status);
    }

    private static final class DelayWindow {
        private final Integer minDelaySeconds;
        private final Integer maxDelaySeconds;

        private DelayWindow(Integer minDelaySeconds, Integer maxDelaySeconds) {
            this.minDelaySeconds = minDelaySeconds;
            this.maxDelaySeconds = maxDelaySeconds;
        }
    }
}
