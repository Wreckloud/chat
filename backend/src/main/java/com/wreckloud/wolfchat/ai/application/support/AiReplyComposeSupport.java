package com.wreckloud.wolfchat.ai.application.support;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

import java.util.function.Function;

/**
 * @Description AI 回复编排支持
 * @Author Wreckloud
 * @Date 2026-03-23
 */
@Component
@RequiredArgsConstructor
public class AiReplyComposeSupport {
    public String appendLobbyDirectedPrompt(String basePrompt,
                                            boolean directedToBot,
                                            Long senderId,
                                            Function<Long, String> displayNameResolver) {
        if (!directedToBot || !StringUtils.hasText(basePrompt)) {
            return basePrompt;
        }
        String result = basePrompt + "\n触发说明：对方在点名你或回复你，请优先正面回应对方。";
        String targetName = displayNameResolver.apply(senderId);
        if (StringUtils.hasText(targetName)) {
            result = result + "\n可直接称呼对方：@" + targetName;
        }
        return result;
    }

    public String appendForumDirectedPrompt(String basePrompt,
                                            boolean directedToBot,
                                            Long directedUserId,
                                            Function<Long, String> displayNameResolver) {
        if (!directedToBot || !StringUtils.hasText(basePrompt)) {
            return basePrompt;
        }
        String result = basePrompt + "\n触发说明：有人在点名你或直接回复你，请优先回应对方。";
        String targetName = displayNameResolver.apply(directedUserId);
        if (StringUtils.hasText(targetName)) {
            result = result + "\n建议称呼：@" + targetName;
        }
        return result;
    }

    public String normalizeLobbyDirectedReply(String reply,
                                              boolean directedToBot,
                                              Long senderId,
                                              Function<Long, String> displayNameResolver) {
        if (!directedToBot) {
            return reply;
        }
        return ensureReplyMention(reply, displayNameResolver.apply(senderId));
    }

    public String normalizeForumDirectedReply(String reply,
                                              boolean directedToBot,
                                              Long directedUserId,
                                              Function<Long, String> displayNameResolver) {
        if (!directedToBot) {
            return reply;
        }
        return ensureForumDirectedPrefix(reply, displayNameResolver.apply(directedUserId));
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
}

