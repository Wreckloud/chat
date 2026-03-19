package com.wreckloud.wolfchat.community.application.support;

import com.wreckloud.wolfchat.chat.media.application.service.ChatMediaService;
import com.wreckloud.wolfchat.common.excption.BaseException;
import com.wreckloud.wolfchat.common.excption.ErrorCode;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;

/**
 * 论坛发帖/回帖请求体归一化与媒体校验。
 */
@Component
@RequiredArgsConstructor
public class ForumPayloadSupport {
    private final ChatMediaService chatMediaService;

    public String normalizeOptionalContent(String text) {
        if (text == null) {
            return "";
        }
        return text.trim();
    }

    public String normalizeOptionalKey(String text) {
        if (text == null) {
            return null;
        }
        String normalized = text.trim();
        return normalized.isEmpty() ? null : normalized;
    }

    public List<String> normalizeImageKeys(List<String> imageKeys, int maxCount) {
        if (imageKeys == null || imageKeys.isEmpty()) {
            return List.of();
        }
        LinkedHashSet<String> uniqueSet = new LinkedHashSet<>();
        for (String item : imageKeys) {
            if (item == null) {
                continue;
            }
            String normalized = item.trim();
            if (normalized.isEmpty()) {
                continue;
            }
            uniqueSet.add(normalized);
        }
        if (uniqueSet.isEmpty()) {
            return List.of();
        }
        if (uniqueSet.size() > maxCount) {
            throw new BaseException(ErrorCode.PARAM_ERROR, "图片数量不能超过" + maxCount + "张");
        }
        return new ArrayList<>(uniqueSet);
    }

    public void validateThreadPayload(Long userId, String content, List<String> imageKeys, String videoKey) {
        boolean hasContent = content != null && !content.isEmpty();
        boolean hasImages = imageKeys != null && !imageKeys.isEmpty();
        boolean hasVideo = videoKey != null && !videoKey.isEmpty();

        if (!hasContent && !hasImages && !hasVideo) {
            throw new BaseException(ErrorCode.PARAM_ERROR, "主题内容不能为空");
        }
        if (hasImages && hasVideo) {
            throw new BaseException(ErrorCode.PARAM_ERROR, "图片和视频不能同时提交");
        }
        if (hasImages) {
            for (String imageKey : imageKeys) {
                chatMediaService.validateForumThreadImageKey(userId, imageKey);
            }
        }
        if (hasVideo) {
            chatMediaService.validateForumThreadVideoKey(userId, videoKey);
        }
    }

    public void validateReplyPayload(Long userId, String content, String imageKey) {
        boolean hasContent = content != null && !content.isEmpty();
        boolean hasImage = imageKey != null && !imageKey.isEmpty();
        if (!hasContent && !hasImage) {
            throw new BaseException(ErrorCode.PARAM_ERROR, "回复内容不能为空");
        }
        if (hasImage) {
            chatMediaService.validateForumReplyImageKey(userId, imageKey);
        }
    }

    public String joinImageKeys(List<String> imageKeys) {
        if (imageKeys == null || imageKeys.isEmpty()) {
            return null;
        }
        return String.join(",", imageKeys);
    }
}

