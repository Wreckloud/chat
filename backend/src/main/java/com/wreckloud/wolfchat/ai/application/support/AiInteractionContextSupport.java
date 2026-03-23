package com.wreckloud.wolfchat.ai.application.support;

import com.wreckloud.wolfchat.account.application.service.UserService;
import com.wreckloud.wolfchat.account.domain.entity.WfUser;
import com.wreckloud.wolfchat.ai.application.service.AiInteractionMemoryService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * @Description AI 互动上下文支持
 * @Author Wreckloud
 * @Date 2026-03-23
 */
@Component
@RequiredArgsConstructor
public class AiInteractionContextSupport {
    private final AiInteractionMemoryService aiInteractionMemoryService;
    private final UserService userService;

    public String appendInteractionMemoryDigest(String baseDigest, String scene, Long botUserId) {
        String interactionDigest = buildInteractionMemoryDigest(scene, botUserId);
        return mergeMemoryDigest(baseDigest, interactionDigest);
    }

    public String resolveUserDisplayName(Long userId) {
        if (!isValidUserId(userId)) {
            return null;
        }
        Map<Long, WfUser> userMap = userService.getUserMap(Set.of(userId));
        return resolveUserDisplayName(userMap.get(userId), userId);
    }

    private String buildInteractionMemoryDigest(String scene, Long botUserId) {
        List<Long> recentUserIds = aiInteractionMemoryService.listRecentUserIds(scene, botUserId, 3);
        String userDigest = buildRecentUserDigest(recentUserIds);
        String topicDigest = aiInteractionMemoryService.buildTopicDigest(scene, botUserId, 3);
        return mergeMemoryDigest(userDigest, topicDigest);
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

    private boolean isValidUserId(Long userId) {
        return userId != null && userId > 0L;
    }
}

