package com.wreckloud.wolfchat.ai.application.service;

import com.wreckloud.wolfchat.chat.lobby.domain.entity.WfLobbyMessage;
import com.wreckloud.wolfchat.chat.message.application.service.MessageMediaService;
import com.wreckloud.wolfchat.chat.message.domain.entity.WfMessage;
import com.wreckloud.wolfchat.community.domain.entity.WfForumReply;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import java.util.ArrayList;
import java.util.List;

/**
 * AI 轻量短期记忆摘要服务（基于已加载上下文，不新增模型调用）。
 */
@Service
@RequiredArgsConstructor
public class AiMemoryDigestService {
    private static final int MAX_ITEMS = 3;
    private static final int MAX_ITEM_CHARS = 36;
    private static final int MIN_USEFUL_CHARS = 2;

    private final MessageMediaService messageMediaService;

    public String buildPrivateDigest(List<WfMessage> recentMessages) {
        List<String> focusItems = new ArrayList<>();
        if (recentMessages != null) {
            for (int i = recentMessages.size() - 1; i >= 0 && focusItems.size() < MAX_ITEMS; i--) {
                WfMessage item = recentMessages.get(i);
                if (item == null) {
                    continue;
                }
                String preview = messageMediaService.buildConversationPreview(item.getMsgType(), item.getContent());
                appendFocusItem(focusItems, preview);
            }
        }
        return joinDigest(focusItems);
    }

    public String buildLobbyDigest(List<WfLobbyMessage> recentMessages) {
        List<String> focusItems = new ArrayList<>();
        if (recentMessages != null) {
            for (int i = recentMessages.size() - 1; i >= 0 && focusItems.size() < MAX_ITEMS; i--) {
                WfLobbyMessage item = recentMessages.get(i);
                if (item == null) {
                    continue;
                }
                String preview = messageMediaService.buildConversationPreview(item.getMsgType(), item.getContent());
                appendFocusItem(focusItems, preview);
            }
        }
        return joinDigest(focusItems);
    }

    public String buildForumDigest(List<WfForumReply> recentReplies) {
        List<String> focusItems = new ArrayList<>();
        if (recentReplies != null) {
            for (int i = recentReplies.size() - 1; i >= 0 && focusItems.size() < MAX_ITEMS; i--) {
                WfForumReply item = recentReplies.get(i);
                if (item == null) {
                    continue;
                }
                appendFocusItem(focusItems, item.getContent());
            }
        }
        return joinDigest(focusItems);
    }

    private void appendFocusItem(List<String> holder, String text) {
        if (!StringUtils.hasText(text)) {
            return;
        }
        String normalized = normalizeText(text);
        if (normalized.length() < MIN_USEFUL_CHARS) {
            return;
        }
        holder.add(normalized);
    }

    private String normalizeText(String text) {
        String normalized = text.replace('\n', ' ').replace('\r', ' ').trim();
        if (normalized.length() <= MAX_ITEM_CHARS) {
            return normalized;
        }
        return normalized.substring(0, MAX_ITEM_CHARS) + "...";
    }

    private String joinDigest(List<String> focusItems) {
        if (focusItems.isEmpty()) {
            return null;
        }
        return "最近对话焦点：" + String.join(" | ", focusItems);
    }
}

