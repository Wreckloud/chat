package com.wreckloud.wolfchat.ai.application.service;

import com.wreckloud.wolfchat.account.application.service.UserService;
import com.wreckloud.wolfchat.account.domain.entity.WfUser;
import com.wreckloud.wolfchat.chat.lobby.domain.entity.WfLobbyMessage;
import com.wreckloud.wolfchat.chat.message.application.service.MessageMediaService;
import com.wreckloud.wolfchat.chat.message.domain.entity.WfMessage;
import com.wreckloud.wolfchat.community.domain.entity.WfForumReply;
import com.wreckloud.wolfchat.community.domain.entity.WfForumThread;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * AI Prompt 组装服务。
 */
@Service
@RequiredArgsConstructor
public class AiPromptBuilderService {
    private static final int MAX_LINE_LENGTH = 180;
    private static final String WOLF_BASELINE = "身份基线：你是社区里的狼系用户，不是助手。";
    private static final String WOLF_STYLE = "表达偏好：自然口语、短句优先，允许情绪和梗，但别故意端着。";
    private static final String WOLF_FEEDBACK = "反馈原则：默认先肯定亮点，再给可执行建议；禁止贬低、嘲笑或打击对方。";
    private static final String WOLF_CONSTRAINT = "边界：不自称AI，不编造未看到的事实，不输出危险或仇恨内容。";

    private final UserService userService;
    private final MessageMediaService messageMediaService;

    public String buildPrivatePrompt(Long botUserId,
                                     Long humanUserId,
                                     List<WfMessage> recentMessages,
                                     String rolePrompt,
                                     String moodDirective,
                                     String memoryDigest) {
        return buildPrivatePrompt(
                botUserId,
                humanUserId,
                recentMessages,
                rolePrompt,
                moodDirective,
                memoryDigest,
                "serious",
                false
        );
    }

    public String buildPrivatePrompt(Long botUserId,
                                     Long humanUserId,
                                     List<WfMessage> recentMessages,
                                     String rolePrompt,
                                     String moodDirective,
                                     String memoryDigest,
                                     String engagementMode,
                                     boolean conversationStalled) {
        StringBuilder prompt = new StringBuilder();
        prompt.append("场景：你正在进行私聊对话。\n");
        prompt.append("角色：你是社区里的狼系用户，语气直接、短句、有网感，可轻微调侃。\n");
        appendWolfBaseline(prompt, "private");
        appendRolePrompt(prompt, rolePrompt);
        appendMoodDirective(prompt, moodDirective);
        appendMemoryDigest(prompt, memoryDigest);
        prompt.append("要求：先接住对方最后一句，再顺势延展，通常1-3句即可。\n");
        prompt.append("可以有态度，不必每次都追问；有合适话头再追问。\n");
        prompt.append("优先让对方感到被理解，再表达你的观点。\n");
        appendPrivateEngagementMode(prompt, engagementMode, conversationStalled);
        prompt.append("注意：不要客服腔，不要长篇说教。\n");
        prompt.append("当前消息窗口如下（按时间顺序）：\n");
        appendPrivateMessageLines(prompt, recentMessages);
        String latestHumanMessage = findLatestPrivateMessageBySender(recentMessages, humanUserId);
        if (StringUtils.hasText(latestHumanMessage)) {
            prompt.append("最后一条对方消息：").append(truncate(latestHumanMessage)).append('\n');
        }
        prompt.append("你是 user#").append(botUserId).append("，对方是 user#").append(humanUserId).append("。\n");
        prompt.append("请仅输出回复正文，不要输出额外标签。");
        return prompt.toString();
    }

    private void appendPrivateEngagementMode(StringBuilder prompt, String engagementMode, boolean conversationStalled) {
        String mode = StringUtils.hasText(engagementMode) ? engagementMode.trim().toLowerCase() : "serious";
        if ("greeting".equals(mode)) {
            prompt.append("互动模式：greeting。先接住招呼，再自然带出一个可继续的话头。\n");
        } else if ("banter".equals(mode)) {
            prompt.append("互动模式：banter。可玩梗和轻吐槽，但别刻意上强度。\n");
        } else {
            prompt.append("互动模式：serious。先回应核心点，再补一句你的看法。\n");
        }
        if (conversationStalled) {
            prompt.append("当前状态：对话有卡壳迹象。请主动给一个轻松切入点把对话续上。\n");
        }
    }

    public String buildLobbyPrompt(Long botUserId,
                                   Long triggerUserId,
                                   List<WfLobbyMessage> recentMessages,
                                   String rolePrompt,
                                   String moodDirective,
                                   String memoryDigest) {
        StringBuilder prompt = new StringBuilder();
        prompt.append("场景：你在公共聊天室参与聊天。\n");
        prompt.append("角色：你是社区里的狼系老用户，会上网、有态度、偶尔接梗。\n");
        appendWolfBaseline(prompt, "lobby");
        appendRolePrompt(prompt, rolePrompt);
        appendMoodDirective(prompt, moodDirective);
        appendMemoryDigest(prompt, memoryDigest);
        prompt.append("要求：结合最近聊天自然接话，通常1-2句，不连续刷屏。\n");
        prompt.append("优先承接当前话题，不要突然换题，不要复读历史消息。\n");
        prompt.append("互动里允许玩梗，但避免打压别人，先接住再补观点。\n");
        prompt.append("注意：不要冒充系统管理员，不要输出“作为AI”。\n");
        prompt.append("最近聊天记录（按时间顺序）：\n");
        appendLobbyMessageLines(prompt, recentMessages);
        String triggerLatest = findLatestLobbyMessageBySender(recentMessages, triggerUserId);
        if (StringUtils.hasText(triggerLatest)) {
            prompt.append("触发用户最近一条：").append(truncate(triggerLatest)).append('\n');
        }
        prompt.append("你是 user#").append(botUserId).append("，触发消息来自 user#").append(triggerUserId).append("。\n");
        prompt.append("请仅输出一条消息正文。");
        return prompt.toString();
    }

    public String buildForumReplyPrompt(Long botUserId,
                                        WfForumThread thread,
                                        WfForumReply triggerReply,
                                        List<WfForumReply> recentReplies,
                                        String rolePrompt,
                                        String moodDirective,
                                        String memoryDigest) {
        StringBuilder prompt = new StringBuilder();
        prompt.append("场景：你在社区主题下回帖。\n");
        prompt.append("角色：你是狼系社区用户，表达直接，有观点，但不过度攻击。\n");
        appendWolfBaseline(prompt, "forum-reply");
        appendRolePrompt(prompt, rolePrompt);
        appendMoodDirective(prompt, moodDirective);
        appendMemoryDigest(prompt, memoryDigest);
        prompt.append("要求：围绕主题给出观点，1-3句，尽量具体，不写公文腔。\n");
        prompt.append("评价他人作品或观点时，先肯定一处亮点，再给改进建议。\n");
        prompt.append("主题标题：").append(truncate(thread == null ? null : thread.getTitle())).append('\n');
        prompt.append("主题正文：").append(truncate(thread == null ? null : thread.getContent())).append('\n');
        if (triggerReply != null) {
            prompt.append("触发回复：").append(truncate(triggerReply.getContent())).append('\n');
        }
        prompt.append("近期楼层（按时间顺序）：\n");
        appendForumReplyLines(prompt, recentReplies);
        prompt.append("你是 user#").append(botUserId).append("。\n");
        prompt.append("仅输出回帖正文，不要输出解释。");
        return prompt.toString();
    }

    private void appendWolfBaseline(StringBuilder prompt, String scene) {
        prompt.append(WOLF_BASELINE).append('\n');
        prompt.append(WOLF_STYLE).append('\n');
        prompt.append(WOLF_FEEDBACK).append('\n');
        prompt.append(WOLF_CONSTRAINT).append('\n');
        if ("private".equals(scene)) {
            prompt.append("私聊细则：可以更贴近对方语气，偶尔冷幽默，但别装懂和灌鸡汤。\n");
            return;
        }
        if ("lobby".equals(scene)) {
            prompt.append("大厅细则：有态度但不抢戏，不要每条都接，不要连发刷存在感。\n");
            return;
        }
        if ("forum-reply".equals(scene)) {
            prompt.append("回帖细则：观点明确、就事论事，可以犀利但必须尊重，不做打击式评价。\n");
            return;
        }
    }

    private void appendRolePrompt(StringBuilder prompt, String rolePrompt) {
        if (!StringUtils.hasText(rolePrompt)) {
            return;
        }
        prompt.append("人格设定：").append(rolePrompt.trim()).append('\n');
    }

    private void appendMoodDirective(StringBuilder prompt, String moodDirective) {
        if (!StringUtils.hasText(moodDirective)) {
            return;
        }
        prompt.append("情绪状态：").append(moodDirective.trim()).append('\n');
    }

    private void appendMemoryDigest(StringBuilder prompt, String memoryDigest) {
        if (!StringUtils.hasText(memoryDigest)) {
            return;
        }
        prompt.append(memoryDigest.trim()).append('\n');
    }

    private void appendPrivateMessageLines(StringBuilder prompt, List<WfMessage> messages) {
        if (messages == null || messages.isEmpty()) {
            prompt.append("- （暂无历史）\n");
            return;
        }
        Set<Long> userIds = new LinkedHashSet<>();
        for (WfMessage message : messages) {
            if (message == null || message.getSenderId() == null || message.getSenderId() <= 0L) {
                continue;
            }
            userIds.add(message.getSenderId());
        }
        Map<Long, WfUser> userMap = userService.getUserMap(userIds);
        for (WfMessage message : messages) {
            if (message == null) {
                continue;
            }
            String name = resolveUserDisplayName(userMap.get(message.getSenderId()), message.getSenderId());
            String preview = messageMediaService.buildConversationPreview(message.getMsgType(), message.getContent());
            prompt.append("- ").append(name).append(": ").append(truncate(preview)).append('\n');
        }
    }

    private void appendLobbyMessageLines(StringBuilder prompt, List<WfLobbyMessage> messages) {
        if (messages == null || messages.isEmpty()) {
            prompt.append("- （暂无历史）\n");
            return;
        }
        Set<Long> userIds = new LinkedHashSet<>();
        for (WfLobbyMessage message : messages) {
            if (message == null || message.getSenderId() == null || message.getSenderId() <= 0L) {
                continue;
            }
            userIds.add(message.getSenderId());
        }
        Map<Long, WfUser> userMap = userService.getUserMap(userIds);
        for (WfLobbyMessage message : messages) {
            if (message == null) {
                continue;
            }
            String name = resolveUserDisplayName(userMap.get(message.getSenderId()), message.getSenderId());
            String preview = messageMediaService.buildConversationPreview(message.getMsgType(), message.getContent());
            prompt.append("- ").append(name).append(": ").append(truncate(preview)).append('\n');
        }
    }

    private void appendForumReplyLines(StringBuilder prompt, List<WfForumReply> replies) {
        if (replies == null || replies.isEmpty()) {
            prompt.append("- （暂无回复）\n");
            return;
        }
        Set<Long> userIds = new LinkedHashSet<>();
        for (WfForumReply reply : replies) {
            if (reply == null || reply.getAuthorId() == null || reply.getAuthorId() <= 0L) {
                continue;
            }
            userIds.add(reply.getAuthorId());
        }
        Map<Long, WfUser> userMap = userService.getUserMap(userIds);
        for (WfForumReply reply : replies) {
            if (reply == null) {
                continue;
            }
            String name = resolveUserDisplayName(userMap.get(reply.getAuthorId()), reply.getAuthorId());
            Integer floorNo = reply.getFloorNo() == null ? 0 : reply.getFloorNo();
            prompt.append("- #").append(floorNo).append(' ')
                    .append(name).append(": ")
                    .append(truncate(reply.getContent()))
                    .append('\n');
        }
    }

    private String resolveUserDisplayName(WfUser user, Long userId) {
        if (user != null && StringUtils.hasText(user.getNickname())) {
            return user.getNickname().trim();
        }
        if (user != null && StringUtils.hasText(user.getWolfNo())) {
            return user.getWolfNo().trim();
        }
        if (userId != null && userId > 0L) {
            return "user#" + userId;
        }
        return "user";
    }

    private String findLatestPrivateMessageBySender(List<WfMessage> messages, Long senderId) {
        if (messages == null || messages.isEmpty() || senderId == null || senderId <= 0L) {
            return null;
        }
        for (int i = messages.size() - 1; i >= 0; i--) {
            WfMessage message = messages.get(i);
            if (message == null || !senderId.equals(message.getSenderId())) {
                continue;
            }
            return messageMediaService.buildConversationPreview(message.getMsgType(), message.getContent());
        }
        return null;
    }

    private String findLatestLobbyMessageBySender(List<WfLobbyMessage> messages, Long senderId) {
        if (messages == null || messages.isEmpty() || senderId == null || senderId <= 0L) {
            return null;
        }
        for (int i = messages.size() - 1; i >= 0; i--) {
            WfLobbyMessage message = messages.get(i);
            if (message == null || !senderId.equals(message.getSenderId())) {
                continue;
            }
            return messageMediaService.buildConversationPreview(message.getMsgType(), message.getContent());
        }
        return null;
    }

    private String truncate(String content) {
        if (!StringUtils.hasText(content)) {
            return "";
        }
        String normalized = content.replace('\n', ' ').replace('\r', ' ').trim();
        if (normalized.length() <= MAX_LINE_LENGTH) {
            return normalized;
        }
        return normalized.substring(0, MAX_LINE_LENGTH) + "...";
    }
}
