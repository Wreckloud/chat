package com.wreckloud.wolfchat.community.application.support;

import com.wreckloud.wolfchat.account.application.service.UserService;
import com.wreckloud.wolfchat.account.domain.entity.WfUser;
import com.wreckloud.wolfchat.community.api.dto.CreateReplyDTO;
import com.wreckloud.wolfchat.community.domain.entity.WfForumReply;
import com.wreckloud.wolfchat.community.domain.entity.WfForumThread;
import com.wreckloud.wolfchat.notice.application.service.UserNoticeService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

import java.util.List;
import java.util.Map;

/**
 * @Description 回帖内容组装与提及处理支持
 * @Author Wreckloud
 * @Date 2026-03-23
 */
@Component
@RequiredArgsConstructor
public class ForumReplyComposeSupport {
    private final ForumPayloadSupport forumPayloadSupport;
    private final UserService userService;
    private final UserNoticeService userNoticeService;

    public ReplyPayload prepareReplyPayload(Long userId, CreateReplyDTO dto, WfForumReply targetReply) {
        String content = forumPayloadSupport.normalizeOptionalContent(dto.getContent());
        content = prependReplyMentionIfNeeded(userId, content, targetReply);
        String imageKey = forumPayloadSupport.normalizeOptionalKey(dto.getImageKey());
        forumPayloadSupport.validateReplyPayload(userId, content, imageKey);
        return new ReplyPayload(content, imageKey);
    }

    public Long resolveReplyParentId(WfForumReply targetReply) {
        if (targetReply == null) {
            return null;
        }
        return targetReply.getId();
    }

    public void notifyReplyTargetIfNeeded(WfForumThread thread, WfForumReply targetReply, Long operatorUserId) {
        if (thread == null || targetReply == null || targetReply.getAuthorId() == null) {
            return;
        }
        Long replyTargetUserId = targetReply.getAuthorId();
        if (replyTargetUserId.equals(operatorUserId)) {
            return;
        }
        if (replyTargetUserId.equals(thread.getAuthorId())) {
            return;
        }
        userNoticeService.notifyReplyReplied(replyTargetUserId, thread.getId(), operatorUserId);
    }

    private String prependReplyMentionIfNeeded(Long userId, String content, WfForumReply targetReply) {
        if (targetReply == null || targetReply.getAuthorId() == null || targetReply.getAuthorId().equals(userId)) {
            return content;
        }
        String displayName = resolveMentionDisplayName(targetReply.getAuthorId());
        if (!StringUtils.hasText(displayName)) {
            return content;
        }

        String normalizedContent = content == null ? "" : content.trim();
        String mentionPrefix = "@" + displayName + " ";
        if (normalizedContent.startsWith(mentionPrefix)) {
            return normalizedContent;
        }
        if (!normalizedContent.isEmpty() && normalizedContent.startsWith("@")) {
            normalizedContent = normalizedContent.replaceFirst("^@\\S+\\s*", "");
        }
        if (normalizedContent.isEmpty()) {
            return mentionPrefix.trim();
        }
        return mentionPrefix + normalizedContent;
    }

    private String resolveMentionDisplayName(Long targetUserId) {
        if (targetUserId == null || targetUserId <= 0L) {
            return "";
        }
        Map<Long, WfUser> userMap = userService.getUserMap(List.of(targetUserId));
        WfUser user = userMap.get(targetUserId);
        if (user == null) {
            return "";
        }
        if (StringUtils.hasText(user.getNickname())) {
            return user.getNickname().trim();
        }
        if (StringUtils.hasText(user.getWolfNo())) {
            return user.getWolfNo().trim();
        }
        return "";
    }

    public static class ReplyPayload {
        private final String content;
        private final String imageKey;

        public ReplyPayload(String content, String imageKey) {
            this.content = content;
            this.imageKey = imageKey;
        }

        public String getContent() {
            return content;
        }

        public String getImageKey() {
            return imageKey;
        }
    }
}

