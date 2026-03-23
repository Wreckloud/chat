package com.wreckloud.wolfchat.community.application.support;

import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import com.wreckloud.wolfchat.common.excption.BaseException;
import com.wreckloud.wolfchat.common.excption.ErrorCode;
import com.wreckloud.wolfchat.community.domain.entity.WfForumThread;
import com.wreckloud.wolfchat.community.domain.enums.ForumThreadStatus;
import com.wreckloud.wolfchat.community.domain.enums.ForumThreadType;
import com.wreckloud.wolfchat.community.infra.mapper.WfForumThreadMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

import java.time.LocalDateTime;

/**
 * @Description 主题写操作支持（创建/草稿/发布/恢复）
 * @Author Wreckloud
 * @Date 2026-03-23
 */
@Component
@RequiredArgsConstructor
public class ForumThreadWriteSupport {
    private static final int THREAD_TITLE_MAX_LENGTH = 120;
    private static final int DRAFT_TITLE_CONTENT_PREVIEW_LENGTH = 20;
    private static final String DEFAULT_DRAFT_TITLE = "未命名草稿";

    private final WfForumThreadMapper wfForumThreadMapper;

    public Long createPublishedThread(Long boardId,
                                      Long userId,
                                      String title,
                                      String content,
                                      String imageKeys,
                                      String videoKey,
                                      String videoPosterKey,
                                      LocalDateTime now) {
        WfForumThread thread = new WfForumThread();
        thread.setBoardId(boardId);
        thread.setAuthorId(userId);
        thread.setTitle(title);
        thread.setContent(content);
        thread.setImageKeys(imageKeys);
        thread.setVideoKey(videoKey);
        thread.setVideoPosterKey(videoPosterKey);
        thread.setThreadType(ForumThreadType.NORMAL);
        thread.setStatus(ForumThreadStatus.NORMAL);
        thread.setIsEssence(false);
        thread.setViewCount(0);
        thread.setReplyCount(0);
        thread.setLastReplyTime(now);
        thread.setEditTime(null);
        assertSingleRow(wfForumThreadMapper.insert(thread));
        return thread.getId();
    }

    public Long createDraftThread(Long boardId,
                                  Long userId,
                                  String title,
                                  String content,
                                  String imageKeys,
                                  String videoKey,
                                  String videoPosterKey,
                                  LocalDateTime now) {
        WfForumThread draft = new WfForumThread();
        draft.setBoardId(boardId);
        draft.setAuthorId(userId);
        draft.setTitle(normalizeDraftTitle(title, content));
        draft.setContent(content);
        draft.setImageKeys(imageKeys);
        draft.setVideoKey(videoKey);
        draft.setVideoPosterKey(videoPosterKey);
        draft.setThreadType(ForumThreadType.NORMAL);
        draft.setStatus(ForumThreadStatus.DRAFT);
        draft.setIsEssence(false);
        draft.setViewCount(0);
        draft.setReplyCount(0);
        draft.setLikeCount(0);
        draft.setLastReplyId(null);
        draft.setLastReplyUserId(null);
        draft.setLastReplyTime(null);
        draft.setEditTime(now);
        assertSingleRow(wfForumThreadMapper.insert(draft));
        return draft.getId();
    }

    public void saveDraftThread(Long threadId,
                                Long userId,
                                String title,
                                String content,
                                String imageKeys,
                                String videoKey,
                                String videoPosterKey,
                                LocalDateTime now) {
        LambdaUpdateWrapper<WfForumThread> updateWrapper = new LambdaUpdateWrapper<>();
        updateWrapper.eq(WfForumThread::getId, threadId)
                .eq(WfForumThread::getAuthorId, userId)
                .eq(WfForumThread::getStatus, ForumThreadStatus.DRAFT)
                .set(WfForumThread::getTitle, normalizeDraftTitle(title, content))
                .set(WfForumThread::getContent, content)
                .set(WfForumThread::getImageKeys, imageKeys)
                .set(WfForumThread::getVideoKey, videoKey)
                .set(WfForumThread::getVideoPosterKey, videoPosterKey)
                .set(WfForumThread::getEditTime, now);
        assertSingleRow(wfForumThreadMapper.update(null, updateWrapper), ErrorCode.FORUM_THREAD_NOT_FOUND);
    }

    public void updatePublishedThreadContent(Long userId,
                                             WfForumThread thread,
                                             String title,
                                             String content,
                                             String imageKeys,
                                             String videoKey,
                                             String videoPosterKey,
                                             boolean publishFromDraft) {
        boolean changed = publishFromDraft
                || !safeEquals(title, thread.getTitle())
                || !safeEquals(content, thread.getContent())
                || !safeEquals(imageKeys, thread.getImageKeys())
                || !safeEquals(videoKey, thread.getVideoKey())
                || !safeEquals(videoPosterKey, thread.getVideoPosterKey());
        if (!changed) {
            return;
        }

        LocalDateTime now = LocalDateTime.now();
        LambdaUpdateWrapper<WfForumThread> updateWrapper = new LambdaUpdateWrapper<>();
        updateWrapper.eq(WfForumThread::getId, thread.getId())
                .eq(WfForumThread::getAuthorId, userId)
                .eq(WfForumThread::getStatus, thread.getStatus())
                .set(WfForumThread::getTitle, title)
                .set(WfForumThread::getContent, content)
                .set(WfForumThread::getImageKeys, imageKeys)
                .set(WfForumThread::getVideoKey, videoKey)
                .set(WfForumThread::getVideoPosterKey, videoPosterKey)
                .set(WfForumThread::getEditTime, now);

        if (publishFromDraft) {
            updateWrapper
                    .set(WfForumThread::getStatus, ForumThreadStatus.NORMAL)
                    .set(WfForumThread::getCreateTime, now)
                    .set(WfForumThread::getLastReplyId, null)
                    .set(WfForumThread::getLastReplyUserId, null)
                    .set(WfForumThread::getLastReplyTime, now)
                    .set(WfForumThread::getThreadType, ForumThreadType.NORMAL)
                    .set(WfForumThread::getIsEssence, false)
                    .set(WfForumThread::getViewCount, 0)
                    .set(WfForumThread::getReplyCount, 0)
                    .set(WfForumThread::getLikeCount, 0);
        }
        assertSingleRow(wfForumThreadMapper.update(null, updateWrapper), ErrorCode.FORUM_THREAD_NOT_FOUND);
    }

    public void restoreDeletedThreadContent(Long userId,
                                            WfForumThread thread,
                                            String title,
                                            String content,
                                            String imageKeys,
                                            String videoKey,
                                            String videoPosterKey) {
        LocalDateTime now = LocalDateTime.now();
        LambdaUpdateWrapper<WfForumThread> updateWrapper = new LambdaUpdateWrapper<>();
        updateWrapper.eq(WfForumThread::getId, thread.getId())
                .eq(WfForumThread::getAuthorId, userId)
                .eq(WfForumThread::getStatus, ForumThreadStatus.DELETED)
                .set(WfForumThread::getTitle, title)
                .set(WfForumThread::getContent, content)
                .set(WfForumThread::getImageKeys, imageKeys)
                .set(WfForumThread::getVideoKey, videoKey)
                .set(WfForumThread::getVideoPosterKey, videoPosterKey)
                .set(WfForumThread::getStatus, ForumThreadStatus.NORMAL)
                .set(WfForumThread::getThreadType, ForumThreadType.NORMAL)
                .set(WfForumThread::getIsEssence, false)
                .set(WfForumThread::getCreateTime, now)
                .set(WfForumThread::getEditTime, null)
                .set(WfForumThread::getViewCount, 0)
                .set(WfForumThread::getReplyCount, 0)
                .set(WfForumThread::getLikeCount, 0)
                .set(WfForumThread::getLastReplyId, null)
                .set(WfForumThread::getLastReplyUserId, null)
                .set(WfForumThread::getLastReplyTime, now);
        assertSingleRow(wfForumThreadMapper.update(null, updateWrapper), ErrorCode.FORUM_THREAD_NOT_FOUND);
    }

    private String normalizeDraftTitle(String title, String content) {
        if (StringUtils.hasText(title)) {
            String normalized = title.trim();
            if (normalized.length() <= THREAD_TITLE_MAX_LENGTH) {
                return normalized;
            }
            return normalized.substring(0, THREAD_TITLE_MAX_LENGTH);
        }
        if (StringUtils.hasText(content)) {
            String normalizedContent = content.replaceAll("\\s+", " ").trim();
            if (!normalizedContent.isEmpty()) {
                int maxLength = Math.min(THREAD_TITLE_MAX_LENGTH, DRAFT_TITLE_CONTENT_PREVIEW_LENGTH);
                if (normalizedContent.length() <= maxLength) {
                    return normalizedContent;
                }
                return normalizedContent.substring(0, maxLength) + "...";
            }
        }
        return DEFAULT_DRAFT_TITLE;
    }

    private boolean safeEquals(String left, String right) {
        if (left == null && right == null) {
            return true;
        }
        if (left == null || right == null) {
            return false;
        }
        return left.equals(right);
    }

    private void assertSingleRow(int affectedRows) {
        if (affectedRows != 1) {
            throw new BaseException(ErrorCode.DATABASE_ERROR);
        }
    }

    private void assertSingleRow(int affectedRows, ErrorCode zeroRowErrorCode) {
        if (affectedRows == 0) {
            throw new BaseException(zeroRowErrorCode);
        }
        if (affectedRows != 1) {
            throw new BaseException(ErrorCode.DATABASE_ERROR);
        }
    }
}

