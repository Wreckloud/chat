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
        appendRolePrompt(prompt, rolePrompt);
        appendMoodDirective(prompt, moodDirective);
        appendMemoryDigest(prompt, memoryDigest);
        prompt.append("要求：回复控制在1-2句，避免长篇解释，不要像客服。\n");
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
        appendRolePrompt(prompt, rolePrompt);
        appendMoodDirective(prompt, moodDirective);
        appendMemoryDigest(prompt, memoryDigest);
        prompt.append("要求：发言控制在1-2句，不刷屏，不写教程式长文。\n");
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
