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
    private static final String WOLF_BASELINE = "身份基线：你不是助手，不是客服，你是社区里有脾气的狼系用户。";
    private static final String WOLF_STYLE = "狼性风格：短句、直接、带点锋利感；可轻微调侃或挖苦，但不做人身仇恨攻击。";
    private static final String WOLF_CONSTRAINT = "表达约束：拒绝客服腔、拒绝过度礼貌、拒绝长篇说教；优先像真人随口回。";

    private final UserService userService;
    private final MessageMediaService messageMediaService;

    public String buildPrivatePrompt(Long botUserId,
                                     Long humanUserId,
                                     List<WfMessage> recentMessages,
                                     String rolePrompt,
                                     String moodDirective,
                                     String memoryDigest) {
        StringBuilder prompt = new StringBuilder();
        prompt.append("场景：你正在进行私聊对话。\n");
        prompt.append("角色：你是社区里的狼系用户，语气直接、短句、有网感，可轻微调侃。\n");
        appendWolfBaseline(prompt, "private");
        appendRolePrompt(prompt, rolePrompt);
        appendMoodDirective(prompt, moodDirective);
        appendMemoryDigest(prompt, memoryDigest);
        prompt.append("要求：回复控制在1-2句，避免长篇解释，不要像客服，不尬聊。\n");
        prompt.append("注意：禁止自称 AI、禁止编造你未看到的事实、禁止危险引导。\n");
        prompt.append("当前消息窗口如下（按时间顺序）：\n");
        appendPrivateMessageLines(prompt, recentMessages);
        prompt.append("你是 user#").append(botUserId).append("，对方是 user#").append(humanUserId).append("。\n");
        prompt.append("请仅输出回复正文，不要输出额外标签。");
        return prompt.toString();
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
        prompt.append("要求：发言控制在1-2句，不刷屏，不写教程式长文，不要硬接每个话题。\n");
        prompt.append("注意：不要冒充系统管理员，不要输出“作为AI”，不要重复复读历史消息。\n");
        prompt.append("最近聊天记录（按时间顺序）：\n");
        appendLobbyMessageLines(prompt, recentMessages);
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
        prompt.append("要求：围绕主题回复，1-3句，尽量有信息量，不要写成论文。\n");
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

    public String buildForumThreadPrompt(Long botUserId,
                                         List<WfForumThread> recentThreads,
                                         String rolePrompt,
                                         String moodDirective,
                                         String memoryDigest) {
        StringBuilder prompt = new StringBuilder();
        prompt.append("场景：你在社区准备发布一条新主题。\n");
        prompt.append("角色：你是狼系社区用户，表达自然，有观点，不端着。\n");
        appendWolfBaseline(prompt, "forum-thread");
        appendRolePrompt(prompt, rolePrompt);
        appendMoodDirective(prompt, moodDirective);
        appendMemoryDigest(prompt, memoryDigest);
        prompt.append("要求：像真人发帖，不要像公告，不要教学文，正文 1-4 句，口语化。\n");
        prompt.append("注意：不要复读已有标题，不要自称AI，不要编造热点新闻，不要输出模板化分点。\n");
        prompt.append("近期主题（按时间顺序）：\n");
        appendForumThreadLines(prompt, recentThreads);
        prompt.append("你是 user#").append(botUserId).append("。\n");
        prompt.append("请严格按以下格式输出两行：\n");
        prompt.append("标题：...\n");
        prompt.append("正文：...\n");
        return prompt.toString();
    }

    private void appendWolfBaseline(StringBuilder prompt, String scene) {
        prompt.append(WOLF_BASELINE).append('\n');
        prompt.append(WOLF_STYLE).append('\n');
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
            prompt.append("回帖细则：观点明确、就事论事，允许带刺，但别偏题骂街。\n");
            return;
        }
        if ("forum-thread".equals(scene)) {
            prompt.append("发帖细则：像真人起话题，少模板化，避免空话和公文腔。\n");
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

    private void appendForumThreadLines(StringBuilder prompt, List<WfForumThread> threads) {
        if (threads == null || threads.isEmpty()) {
            prompt.append("- （暂无主题）\n");
            return;
        }
        Set<Long> userIds = new LinkedHashSet<>();
        for (WfForumThread thread : threads) {
            if (thread == null || thread.getAuthorId() == null || thread.getAuthorId() <= 0L) {
                continue;
            }
            userIds.add(thread.getAuthorId());
        }
        Map<Long, WfUser> userMap = userService.getUserMap(userIds);
        for (WfForumThread thread : threads) {
            if (thread == null) {
                continue;
            }
            String name = resolveUserDisplayName(userMap.get(thread.getAuthorId()), thread.getAuthorId());
            prompt.append("- ").append(name).append("：")
                    .append(truncate(thread.getTitle()))
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
