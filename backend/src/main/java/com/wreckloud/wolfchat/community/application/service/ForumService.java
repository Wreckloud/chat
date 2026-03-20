package com.wreckloud.wolfchat.community.application.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import com.wreckloud.wolfchat.account.application.service.UserAchievementService;
import com.wreckloud.wolfchat.account.application.service.UserService;
import com.wreckloud.wolfchat.account.domain.entity.WfUser;
import com.wreckloud.wolfchat.common.excption.BaseException;
import com.wreckloud.wolfchat.common.excption.ErrorCode;
import com.wreckloud.wolfchat.community.api.dto.CreateReplyDTO;
import com.wreckloud.wolfchat.community.api.dto.CreateThreadDTO;
import com.wreckloud.wolfchat.community.api.dto.SaveThreadDraftDTO;
import com.wreckloud.wolfchat.community.api.vo.ForumReplyPageVO;
import com.wreckloud.wolfchat.community.api.vo.ForumReplyVO;
import com.wreckloud.wolfchat.community.api.vo.ForumThreadDetailVO;
import com.wreckloud.wolfchat.community.api.vo.ForumThreadEditorVO;
import com.wreckloud.wolfchat.community.api.vo.ForumThreadPageVO;
import com.wreckloud.wolfchat.community.api.vo.ForumThreadVO;
import com.wreckloud.wolfchat.community.api.dto.UpdateThreadDTO;
import com.wreckloud.wolfchat.community.application.support.ForumPayloadSupport;
import com.wreckloud.wolfchat.community.domain.constant.ForumModerationLogConstants;
import com.wreckloud.wolfchat.community.domain.entity.WfForumBoard;
import com.wreckloud.wolfchat.community.domain.entity.WfForumReply;
import com.wreckloud.wolfchat.community.domain.entity.WfForumReplyLike;
import com.wreckloud.wolfchat.community.domain.entity.WfForumThread;
import com.wreckloud.wolfchat.community.domain.entity.WfForumThreadLike;
import com.wreckloud.wolfchat.community.domain.enums.ForumReplyStatus;
import com.wreckloud.wolfchat.community.domain.enums.ForumThreadStatus;
import com.wreckloud.wolfchat.community.domain.enums.ForumThreadType;
import com.wreckloud.wolfchat.community.infra.mapper.WfForumBoardMapper;
import com.wreckloud.wolfchat.community.infra.mapper.WfForumReplyLikeMapper;
import com.wreckloud.wolfchat.community.infra.mapper.WfForumReplyMapper;
import com.wreckloud.wolfchat.community.infra.mapper.WfForumThreadLikeMapper;
import com.wreckloud.wolfchat.community.infra.mapper.WfForumThreadMapper;
import com.wreckloud.wolfchat.notice.application.service.UserNoticeService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * 论坛门面服务：读操作委托给查询服务，写操作在此统一编排。
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class ForumService {
    private static final int THREAD_IMAGE_MAX_COUNT = 9;
    private static final int THREAD_TITLE_MAX_LENGTH = 120;
    private static final int DRAFT_TITLE_CONTENT_PREVIEW_LENGTH = 20;
    private static final String DEFAULT_DRAFT_TITLE = "未命名草稿";

    private final ForumQueryService forumQueryService;
    private final ForumContentMaintenanceService forumContentMaintenanceService;
    private final ForumModerationLogService forumModerationLogService;
    private final WfForumBoardMapper wfForumBoardMapper;
    private final WfForumThreadMapper wfForumThreadMapper;
    private final WfForumReplyMapper wfForumReplyMapper;
    private final WfForumThreadLikeMapper wfForumThreadLikeMapper;
    private final WfForumReplyLikeMapper wfForumReplyLikeMapper;
    private final ForumPayloadSupport forumPayloadSupport;
    private final UserAchievementService userAchievementService;
    private final UserService userService;
    private final UserNoticeService userNoticeService;

    public ForumThreadPageVO listFeedThreads(Long userId, long page, long size, String tab) {
        return forumQueryService.listFeedThreads(userId, page, size, tab);
    }

    public ForumThreadPageVO listSearchThreads(Long userId, long page, long size, String keyword) {
        return forumQueryService.listSearchThreads(userId, page, size, keyword);
    }

    public ForumThreadEditorVO getLatestThreadDraft(Long userId) {
        return forumQueryService.getLatestDraftForAuthor(userId);
    }

    public ForumThreadEditorVO getEditableThread(Long userId, Long threadId) {
        WfForumThread thread = forumQueryService.getAuthorThreadOrThrow(userId, threadId);
        validateEditableThreadStatus(thread.getStatus());
        return forumQueryService.getThreadEditorVO(userId, threadId);
    }

    @Transactional(rollbackFor = Exception.class)
    public ForumThreadVO createThread(Long userId, CreateThreadDTO dto) {
        WfForumBoard board = forumQueryService.resolveDefaultBoardForPostingOrThrow();
        ThreadPayload payload = normalizeThreadPayload(
                userId,
                dto.getTitle(),
                dto.getContent(),
                dto.getImageKeys(),
                dto.getVideoKey(),
                dto.getVideoPosterKey(),
                false
        );

        LocalDateTime now = LocalDateTime.now();
        WfForumThread thread = new WfForumThread();
        thread.setBoardId(board.getId());
        thread.setAuthorId(userId);
        thread.setTitle(payload.title);
        thread.setContent(payload.content);
        thread.setImageKeys(forumPayloadSupport.joinImageKeys(payload.imageKeys));
        thread.setVideoKey(payload.videoKey);
        thread.setVideoPosterKey(payload.videoPosterKey);
        thread.setThreadType(ForumThreadType.NORMAL);
        thread.setStatus(ForumThreadStatus.NORMAL);
        thread.setIsEssence(false);
        thread.setViewCount(0);
        thread.setReplyCount(0);
        thread.setLastReplyTime(now);
        thread.setEditTime(null);

        assertSingleRow(wfForumThreadMapper.insert(thread));
        updateBoardOnThreadCreate(board.getId(), thread.getId(), now);
        userAchievementService.grantFirstPostAchievement(userId);
        return forumQueryService.buildThreadVO(userId, thread.getId());
    }

    @Transactional(rollbackFor = Exception.class)
    public ForumThreadEditorVO saveThreadDraft(Long userId, SaveThreadDraftDTO dto) {
        WfForumBoard board = forumQueryService.resolveDefaultBoardForPostingOrThrow();
        ThreadPayload payload = normalizeThreadPayload(
                userId,
                dto.getTitle(),
                dto.getContent(),
                dto.getImageKeys(),
                dto.getVideoKey(),
                dto.getVideoPosterKey(),
                true
        );

        Long threadId = dto.getThreadId();
        LocalDateTime now = LocalDateTime.now();
        if (threadId == null || threadId <= 0L) {
            WfForumThread draft = new WfForumThread();
            draft.setBoardId(board.getId());
            draft.setAuthorId(userId);
            draft.setTitle(normalizeDraftTitle(payload.title, payload.content));
            draft.setContent(payload.content);
            draft.setImageKeys(forumPayloadSupport.joinImageKeys(payload.imageKeys));
            draft.setVideoKey(payload.videoKey);
            draft.setVideoPosterKey(payload.videoPosterKey);
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
            return forumQueryService.getThreadEditorVO(userId, draft.getId());
        }

        WfForumThread thread = forumQueryService.getAuthorThreadOrThrow(userId, threadId);
        if (!ForumThreadStatus.DRAFT.equals(thread.getStatus())) {
            throw new BaseException(ErrorCode.PARAM_ERROR, "仅支持保存草稿主题");
        }
        LambdaUpdateWrapper<WfForumThread> updateWrapper = new LambdaUpdateWrapper<>();
        updateWrapper.eq(WfForumThread::getId, threadId)
                .eq(WfForumThread::getAuthorId, userId)
                .eq(WfForumThread::getStatus, ForumThreadStatus.DRAFT)
                .set(WfForumThread::getTitle, normalizeDraftTitle(payload.title, payload.content))
                .set(WfForumThread::getContent, payload.content)
                .set(WfForumThread::getImageKeys, forumPayloadSupport.joinImageKeys(payload.imageKeys))
                .set(WfForumThread::getVideoKey, payload.videoKey)
                .set(WfForumThread::getVideoPosterKey, payload.videoPosterKey)
                .set(WfForumThread::getEditTime, now);
        assertSingleRow(wfForumThreadMapper.update(null, updateWrapper), ErrorCode.FORUM_THREAD_NOT_FOUND);
        return forumQueryService.getThreadEditorVO(userId, threadId);
    }

    @Transactional(rollbackFor = Exception.class)
    public ForumThreadVO updateThread(Long userId, Long threadId, UpdateThreadDTO dto) {
        WfForumThread thread = forumQueryService.getAuthorThreadOrThrow(userId, threadId);
        if (!isPublishedThreadStatus(thread.getStatus())) {
            throw new BaseException(ErrorCode.PARAM_ERROR, "仅支持编辑已发布主题");
        }

        ThreadPayload payload = normalizeThreadPayload(
                userId,
                dto.getTitle(),
                dto.getContent(),
                dto.getImageKeys(),
                dto.getVideoKey(),
                dto.getVideoPosterKey(),
                false
        );
        updatePublishedThreadContent(userId, thread, payload, false);
        return forumQueryService.buildThreadVO(userId, threadId);
    }

    @Transactional(rollbackFor = Exception.class)
    public ForumThreadVO publishThreadDraft(Long userId, Long threadId, UpdateThreadDTO dto) {
        WfForumThread thread = forumQueryService.getAuthorThreadOrThrow(userId, threadId);
        if (!ForumThreadStatus.DRAFT.equals(thread.getStatus())) {
            throw new BaseException(ErrorCode.PARAM_ERROR, "仅支持发布草稿主题");
        }

        ThreadPayload payload = normalizeThreadPayload(
                userId,
                dto.getTitle(),
                dto.getContent(),
                dto.getImageKeys(),
                dto.getVideoKey(),
                dto.getVideoPosterKey(),
                false
        );
        updatePublishedThreadContent(userId, thread, payload, true);
        forumContentMaintenanceService.refreshBoardStatsOrThrow(thread.getBoardId());
        userAchievementService.grantFirstPostAchievement(userId);
        return forumQueryService.buildThreadVO(userId, threadId);
    }

    @Transactional(rollbackFor = Exception.class)
    public ForumThreadVO restoreThread(Long userId, Long threadId, UpdateThreadDTO dto) {
        WfForumThread thread = forumQueryService.getAuthorThreadOrThrow(userId, threadId);
        if (!ForumThreadStatus.DELETED.equals(thread.getStatus())) {
            throw new BaseException(ErrorCode.PARAM_ERROR, "仅支持重新发布垃圾站主题");
        }

        ThreadPayload payload = normalizeThreadPayload(
                userId,
                dto.getTitle(),
                dto.getContent(),
                dto.getImageKeys(),
                dto.getVideoKey(),
                dto.getVideoPosterKey(),
                false
        );

        restoreDeletedThreadContent(userId, thread, payload);
        forumContentMaintenanceService.refreshBoardStatsOrThrow(thread.getBoardId());
        return forumQueryService.buildThreadVO(userId, threadId);
    }

    public ForumThreadDetailVO getThreadDetail(Long userId, Long threadId) {
        return forumQueryService.getThreadDetail(userId, threadId);
    }

    @Transactional(rollbackFor = Exception.class)
    public void increaseThreadView(Long threadId) {
        LambdaUpdateWrapper<WfForumThread> updateWrapper = new LambdaUpdateWrapper<>();
        updateWrapper.eq(WfForumThread::getId, threadId)
                .in(WfForumThread::getStatus, List.of(ForumThreadStatus.NORMAL, ForumThreadStatus.LOCKED))
                .setSql("view_count = view_count + 1");
        assertSingleRow(wfForumThreadMapper.update(null, updateWrapper), ErrorCode.FORUM_THREAD_NOT_FOUND);
    }

    @Transactional(rollbackFor = Exception.class)
    public void updateThreadLikeStatus(Long userId, Long threadId, boolean liked) {
        WfForumThread thread = forumQueryService.getVisibleThreadOrThrow(threadId);
        if (liked) {
            boolean likedNow = likeThread(userId, thread.getId());
            if (likedNow) {
                userNoticeService.notifyThreadLiked(thread.getAuthorId(), thread.getId(), userId);
            }
            return;
        }
        unlikeThread(userId, thread.getId());
    }

    public ForumReplyPageVO listThreadReplies(Long userId, Long threadId, long page, long size, String sort) {
        return forumQueryService.listThreadReplies(userId, threadId, page, size, sort);
    }

    @Transactional(rollbackFor = Exception.class)
    public ForumReplyVO createReply(Long userId, Long threadId, CreateReplyDTO dto) {
        Long requestQuoteReplyId = dto.getQuoteReplyId();
        log.info("开始发布回帖: userId={}, threadId={}, quoteReplyId={}, hasContent={}, hasImage={}",
                userId,
                threadId,
                requestQuoteReplyId,
                dto.getContent() != null && !dto.getContent().trim().isEmpty(),
                dto.getImageKey() != null && !dto.getImageKey().trim().isEmpty());
        WfForumThread thread = getThreadForReplyOrThrow(threadId);

        WfForumReply targetReply = null;
        if (requestQuoteReplyId != null) {
            targetReply = forumQueryService.getQuoteReplyOrThrow(threadId, requestQuoteReplyId);
        }

        String content = forumPayloadSupport.normalizeOptionalContent(dto.getContent());
        content = prependReplyMentionIfNeeded(userId, content, targetReply);
        String imageKey = forumPayloadSupport.normalizeOptionalKey(dto.getImageKey());
        forumPayloadSupport.validateReplyPayload(userId, content, imageKey);

        int floorNo = getNextFloorNo(threadId);
        LocalDateTime now = LocalDateTime.now();

        WfForumReply reply = new WfForumReply();
        reply.setThreadId(threadId);
        reply.setFloorNo(floorNo);
        reply.setAuthorId(userId);
        reply.setContent(content);
        reply.setImageKey(imageKey);
        reply.setQuoteReplyId(resolveReplyParentId(targetReply));
        reply.setStatus(ForumReplyStatus.NORMAL);

        assertSingleRow(wfForumReplyMapper.insert(reply));
        updateThreadOnReply(threadId, userId, reply.getId(), now);
        updateBoardOnReply(thread.getBoardId(), threadId, now);
        userAchievementService.grantFirstReplyAchievement(userId);
        userNoticeService.notifyThreadReplied(thread.getAuthorId(), thread.getId(), userId);
        notifyReplyTargetIfNeeded(thread, targetReply, userId);
        log.info("回帖发布成功: userId={}, threadId={}, replyId={}", userId, threadId, reply.getId());
        return forumQueryService.buildReplyVO(userId, reply.getId());
    }

    @Transactional(rollbackFor = Exception.class)
    public void updateReplyLikeStatus(Long userId, Long replyId, boolean liked) {
        WfForumReply reply = forumQueryService.getVisibleReplyOrThrow(replyId);
        if (liked) {
            boolean likedNow = likeReply(userId, reply.getId());
            if (likedNow) {
                userNoticeService.notifyReplyLiked(reply.getAuthorId(), reply.getThreadId(), userId);
            }
            return;
        }
        unlikeReply(userId, reply.getId());
    }

    @Transactional(rollbackFor = Exception.class)
    public void updateThreadLockStatus(Long userId, Long threadId, boolean locked) {
        WfForumThread thread = forumQueryService.getVisibleThreadOrThrow(threadId);
        validateThreadAuthor(userId, thread);

        ForumThreadStatus targetStatus = locked ? ForumThreadStatus.LOCKED : ForumThreadStatus.NORMAL;
        if (targetStatus.equals(thread.getStatus())) {
            return;
        }

        LambdaUpdateWrapper<WfForumThread> updateWrapper = buildThreadAuthorUpdateWrapper(userId, threadId);
        updateWrapper
                .eq(WfForumThread::getStatus, thread.getStatus())
                .set(WfForumThread::getStatus, targetStatus);
        assertSingleRow(wfForumThreadMapper.update(null, updateWrapper));

        String action = locked ? ForumModerationLogConstants.ACTION_LOCK_THREAD : ForumModerationLogConstants.ACTION_UNLOCK_THREAD;
        recordThreadAuthorAction(userId, threadId, action);
    }

    @Transactional(rollbackFor = Exception.class)
    public void updateThreadStickyStatus(Long userId, Long threadId, boolean sticky) {
        WfForumThread thread = forumQueryService.getVisibleThreadOrThrow(threadId);
        validateThreadAuthor(userId, thread);
        validateStickyOperation(thread);

        ForumThreadType targetType = sticky ? ForumThreadType.STICKY : ForumThreadType.NORMAL;
        if (targetType.equals(thread.getThreadType())) {
            return;
        }

        LambdaUpdateWrapper<WfForumThread> updateWrapper = buildThreadAuthorUpdateWrapper(userId, threadId);
        updateWrapper
                .eq(WfForumThread::getThreadType, thread.getThreadType())
                .set(WfForumThread::getThreadType, targetType);
        assertSingleRow(wfForumThreadMapper.update(null, updateWrapper));

        String action = sticky ? ForumModerationLogConstants.ACTION_STICKY_THREAD : ForumModerationLogConstants.ACTION_UNSTICKY_THREAD;
        recordThreadAuthorAction(userId, threadId, action);
    }

    @Transactional(rollbackFor = Exception.class)
    public void updateThreadEssenceStatus(Long userId, Long threadId, boolean essence) {
        WfForumThread thread = forumQueryService.getVisibleThreadOrThrow(threadId);
        validateThreadAuthor(userId, thread);

        boolean currentEssence = Boolean.TRUE.equals(thread.getIsEssence());
        if (currentEssence == essence) {
            return;
        }

        LambdaUpdateWrapper<WfForumThread> updateWrapper = buildThreadAuthorUpdateWrapper(userId, threadId);
        updateWrapper
                .set(WfForumThread::getIsEssence, essence);
        assertSingleRow(wfForumThreadMapper.update(null, updateWrapper));

        String action = essence ? ForumModerationLogConstants.ACTION_ESSENCE_THREAD : ForumModerationLogConstants.ACTION_UNESSENCE_THREAD;
        recordThreadAuthorAction(userId, threadId, action);
    }

    @Transactional(rollbackFor = Exception.class)
    public void deleteThread(Long userId, Long threadId) {
        WfForumThread thread = forumQueryService.getVisibleThreadOrThrow(threadId);
        validateThreadAuthor(userId, thread);

        LambdaUpdateWrapper<WfForumThread> threadUpdateWrapper = new LambdaUpdateWrapper<>();
        threadUpdateWrapper.eq(WfForumThread::getId, threadId)
                .eq(WfForumThread::getAuthorId, userId)
                .in(WfForumThread::getStatus, List.of(ForumThreadStatus.NORMAL, ForumThreadStatus.LOCKED))
                .set(WfForumThread::getStatus, ForumThreadStatus.DELETED);
        assertSingleRow(wfForumThreadMapper.update(null, threadUpdateWrapper));

        LambdaUpdateWrapper<WfForumReply> replyUpdateWrapper = new LambdaUpdateWrapper<>();
        replyUpdateWrapper.eq(WfForumReply::getThreadId, threadId)
                .eq(WfForumReply::getStatus, ForumReplyStatus.NORMAL)
                .set(WfForumReply::getStatus, ForumReplyStatus.DELETED);
        wfForumReplyMapper.update(null, replyUpdateWrapper);

        forumContentMaintenanceService.refreshBoardStatsOrThrow(thread.getBoardId());
        recordThreadAuthorAction(userId, threadId, ForumModerationLogConstants.ACTION_DELETE_THREAD);
    }

    @Transactional(rollbackFor = Exception.class)
    public void purgeThread(Long userId, Long threadId) {
        WfForumThread thread = forumQueryService.getAuthorThreadOrThrow(userId, threadId);
        if (!ForumThreadStatus.DELETED.equals(thread.getStatus())) {
            throw new BaseException(ErrorCode.PARAM_ERROR, "仅支持彻底删除垃圾站主题");
        }

        LambdaUpdateWrapper<WfForumThread> updateWrapper = new LambdaUpdateWrapper<>();
        updateWrapper.eq(WfForumThread::getId, threadId)
                .eq(WfForumThread::getAuthorId, userId)
                .eq(WfForumThread::getStatus, ForumThreadStatus.DELETED)
                .set(WfForumThread::getStatus, ForumThreadStatus.PURGED);
        assertSingleRow(wfForumThreadMapper.update(null, updateWrapper), ErrorCode.FORUM_THREAD_NOT_FOUND);
    }

    @Transactional(rollbackFor = Exception.class)
    public void deleteReply(Long userId, Long replyId) {
        WfForumReply reply = forumQueryService.getVisibleReplyOrThrow(replyId);
        WfForumThread thread = forumQueryService.getVisibleThreadOrThrow(reply.getThreadId());
        validateReplyDeletePermission(userId, reply, thread);

        LambdaUpdateWrapper<WfForumReply> replyUpdateWrapper = new LambdaUpdateWrapper<>();
        replyUpdateWrapper.eq(WfForumReply::getId, replyId)
                .eq(WfForumReply::getStatus, ForumReplyStatus.NORMAL)
                .set(WfForumReply::getStatus, ForumReplyStatus.DELETED);
        assertSingleRow(wfForumReplyMapper.update(null, replyUpdateWrapper));

        forumContentMaintenanceService.refreshThreadLastReply(thread.getId());
        forumContentMaintenanceService.refreshBoardStatsOrThrow(thread.getBoardId());
        forumModerationLogService.record(
                userId,
                ForumModerationLogConstants.TARGET_REPLY,
                replyId,
                ForumModerationLogConstants.ACTION_DELETE_REPLY,
                ForumModerationLogConstants.REASON_AUTHOR_OPERATION
        );
    }

    private boolean likeThread(Long userId, Long threadId) {
        if (existsThreadLike(userId, threadId)) {
            return false;
        }
        WfForumThreadLike like = new WfForumThreadLike();
        like.setThreadId(threadId);
        like.setUserId(userId);
        assertSingleRow(wfForumThreadLikeMapper.insert(like));

        LambdaUpdateWrapper<WfForumThread> updateWrapper = new LambdaUpdateWrapper<>();
        updateWrapper.eq(WfForumThread::getId, threadId)
                .in(WfForumThread::getStatus, List.of(ForumThreadStatus.NORMAL, ForumThreadStatus.LOCKED))
                .setSql("like_count = like_count + 1");
        assertSingleRow(wfForumThreadMapper.update(null, updateWrapper), ErrorCode.FORUM_THREAD_NOT_FOUND);
        return true;
    }

    private void unlikeThread(Long userId, Long threadId) {
        LambdaQueryWrapper<WfForumThreadLike> deleteWrapper = new LambdaQueryWrapper<>();
        deleteWrapper.eq(WfForumThreadLike::getUserId, userId)
                .eq(WfForumThreadLike::getThreadId, threadId);
        int deletedRows = wfForumThreadLikeMapper.delete(deleteWrapper);
        if (deletedRows == 0) {
            return;
        }

        LambdaUpdateWrapper<WfForumThread> updateWrapper = new LambdaUpdateWrapper<>();
        updateWrapper.eq(WfForumThread::getId, threadId)
                .in(WfForumThread::getStatus, List.of(ForumThreadStatus.NORMAL, ForumThreadStatus.LOCKED))
                .setSql("like_count = IF(like_count > 0, like_count - 1, 0)");
        assertSingleRow(wfForumThreadMapper.update(null, updateWrapper), ErrorCode.FORUM_THREAD_NOT_FOUND);
    }

    private boolean likeReply(Long userId, Long replyId) {
        if (existsReplyLike(userId, replyId)) {
            return false;
        }
        WfForumReplyLike like = new WfForumReplyLike();
        like.setReplyId(replyId);
        like.setUserId(userId);
        assertSingleRow(wfForumReplyLikeMapper.insert(like));

        LambdaUpdateWrapper<WfForumReply> updateWrapper = new LambdaUpdateWrapper<>();
        updateWrapper.eq(WfForumReply::getId, replyId)
                .eq(WfForumReply::getStatus, ForumReplyStatus.NORMAL)
                .setSql("like_count = like_count + 1");
        assertSingleRow(wfForumReplyMapper.update(null, updateWrapper), ErrorCode.FORUM_REPLY_NOT_FOUND);
        return true;
    }

    private void unlikeReply(Long userId, Long replyId) {
        LambdaQueryWrapper<WfForumReplyLike> deleteWrapper = new LambdaQueryWrapper<>();
        deleteWrapper.eq(WfForumReplyLike::getUserId, userId)
                .eq(WfForumReplyLike::getReplyId, replyId);
        int deletedRows = wfForumReplyLikeMapper.delete(deleteWrapper);
        if (deletedRows == 0) {
            return;
        }

        LambdaUpdateWrapper<WfForumReply> updateWrapper = new LambdaUpdateWrapper<>();
        updateWrapper.eq(WfForumReply::getId, replyId)
                .eq(WfForumReply::getStatus, ForumReplyStatus.NORMAL)
                .setSql("like_count = IF(like_count > 0, like_count - 1, 0)");
        assertSingleRow(wfForumReplyMapper.update(null, updateWrapper), ErrorCode.FORUM_REPLY_NOT_FOUND);
    }

    private boolean existsThreadLike(Long userId, Long threadId) {
        LambdaQueryWrapper<WfForumThreadLike> queryWrapper = new LambdaQueryWrapper<>();
        queryWrapper.eq(WfForumThreadLike::getUserId, userId)
                .eq(WfForumThreadLike::getThreadId, threadId)
                .last("LIMIT 1");
        Long count = wfForumThreadLikeMapper.selectCount(queryWrapper);
        return count != null && count > 0;
    }

    private boolean existsReplyLike(Long userId, Long replyId) {
        LambdaQueryWrapper<WfForumReplyLike> queryWrapper = new LambdaQueryWrapper<>();
        queryWrapper.eq(WfForumReplyLike::getUserId, userId)
                .eq(WfForumReplyLike::getReplyId, replyId)
                .last("LIMIT 1");
        Long count = wfForumReplyLikeMapper.selectCount(queryWrapper);
        return count != null && count > 0;
    }

    private WfForumThread getThreadForReplyOrThrow(Long threadId) {
        WfForumThread thread = forumQueryService.getVisibleThreadOrThrow(threadId);
        if (ForumThreadStatus.LOCKED.equals(thread.getStatus())) {
            throw new BaseException(ErrorCode.FORUM_THREAD_LOCKED);
        }
        return thread;
    }

    private void validateThreadAuthor(Long userId, WfForumThread thread) {
        if (!userId.equals(thread.getAuthorId())) {
            throw new BaseException(ErrorCode.FORUM_OPERATION_FORBIDDEN);
        }
    }

    private void validateReplyDeletePermission(Long userId, WfForumReply reply, WfForumThread thread) {
        boolean isReplyAuthor = userId.equals(reply.getAuthorId());
        boolean isThreadAuthor = userId.equals(thread.getAuthorId());
        if (!isReplyAuthor && !isThreadAuthor) {
            throw new BaseException(ErrorCode.FORUM_OPERATION_FORBIDDEN);
        }
    }

    private void validateStickyOperation(WfForumThread thread) {
        if (ForumThreadType.ANNOUNCEMENT.equals(thread.getThreadType())) {
            throw new BaseException(ErrorCode.PARAM_ERROR, "公告主题暂不支持置顶切换");
        }
    }

    private int getNextFloorNo(Long threadId) {
        Integer maxFloorNo = wfForumReplyMapper.selectMaxFloorNo(threadId);
        if (maxFloorNo == null || maxFloorNo < 1) {
            return 2;
        }
        return maxFloorNo + 1;
    }

    private LambdaUpdateWrapper<WfForumThread> buildThreadAuthorUpdateWrapper(Long userId, Long threadId) {
        LambdaUpdateWrapper<WfForumThread> updateWrapper = new LambdaUpdateWrapper<>();
        updateWrapper.eq(WfForumThread::getId, threadId)
                .eq(WfForumThread::getAuthorId, userId)
                .in(WfForumThread::getStatus, List.of(ForumThreadStatus.NORMAL, ForumThreadStatus.LOCKED));
        return updateWrapper;
    }

    private void recordThreadAuthorAction(Long userId, Long threadId, String action) {
        forumModerationLogService.record(
                userId,
                ForumModerationLogConstants.TARGET_THREAD,
                threadId,
                action,
                ForumModerationLogConstants.REASON_AUTHOR_OPERATION
        );
    }

    private void updateBoardOnThreadCreate(Long boardId, Long threadId, LocalDateTime now) {
        LambdaUpdateWrapper<WfForumBoard> updateWrapper = new LambdaUpdateWrapper<>();
        updateWrapper.eq(WfForumBoard::getId, boardId)
                .setSql("thread_count = thread_count + 1")
                .set(WfForumBoard::getLastThreadId, threadId)
                .set(WfForumBoard::getLastReplyTime, now);
        assertSingleRow(wfForumBoardMapper.update(null, updateWrapper));
    }

    private void updateThreadOnReply(Long threadId, Long userId, Long replyId, LocalDateTime now) {
        LambdaUpdateWrapper<WfForumThread> updateWrapper = new LambdaUpdateWrapper<>();
        updateWrapper.eq(WfForumThread::getId, threadId)
                .eq(WfForumThread::getStatus, ForumThreadStatus.NORMAL)
                .setSql("reply_count = reply_count + 1")
                .set(WfForumThread::getLastReplyId, replyId)
                .set(WfForumThread::getLastReplyUserId, userId)
                .set(WfForumThread::getLastReplyTime, now);
        assertSingleRow(wfForumThreadMapper.update(null, updateWrapper), ErrorCode.FORUM_THREAD_LOCKED);
    }

    private void updateBoardOnReply(Long boardId, Long threadId, LocalDateTime now) {
        LambdaUpdateWrapper<WfForumBoard> updateWrapper = new LambdaUpdateWrapper<>();
        updateWrapper.eq(WfForumBoard::getId, boardId)
                .setSql("reply_count = reply_count + 1")
                .set(WfForumBoard::getLastThreadId, threadId)
                .set(WfForumBoard::getLastReplyTime, now);
        assertSingleRow(wfForumBoardMapper.update(null, updateWrapper));
    }

    private Long resolveReplyParentId(WfForumReply targetReply) {
        if (targetReply == null) {
            return null;
        }
        return targetReply.getId();
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

    private void notifyReplyTargetIfNeeded(WfForumThread thread, WfForumReply targetReply, Long operatorUserId) {
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

    private ThreadPayload normalizeThreadPayload(Long userId,
                                                 String title,
                                                 String content,
                                                 List<String> imageKeys,
                                                 String videoKey,
                                                 String videoPosterKey,
                                                 boolean draftMode) {
        String normalizedTitle = title == null ? "" : title.trim();
        if (!draftMode && !StringUtils.hasText(normalizedTitle)) {
            throw new BaseException(ErrorCode.PARAM_ERROR, "主题标题不能为空");
        }
        if (normalizedTitle.length() > THREAD_TITLE_MAX_LENGTH) {
            throw new BaseException(ErrorCode.PARAM_ERROR, "主题标题长度不能超过120个字符");
        }

        String normalizedContent = forumPayloadSupport.normalizeOptionalContent(content);
        List<String> normalizedImageKeys = forumPayloadSupport.normalizeImageKeys(imageKeys, THREAD_IMAGE_MAX_COUNT);
        String normalizedVideoKey = forumPayloadSupport.normalizeOptionalKey(videoKey);
        String normalizedVideoPosterKey = forumPayloadSupport.normalizeOptionalKey(videoPosterKey);

        if (draftMode) {
            forumPayloadSupport.validateThreadDraftPayload(
                    userId,
                    normalizedImageKeys,
                    normalizedVideoKey,
                    normalizedVideoPosterKey
            );
        } else {
            forumPayloadSupport.validateThreadPayload(
                    userId,
                    normalizedContent,
                    normalizedImageKeys,
                    normalizedVideoKey,
                    normalizedVideoPosterKey
            );
        }
        return new ThreadPayload(
                normalizedTitle,
                normalizedContent,
                normalizedImageKeys,
                normalizedVideoKey,
                normalizedVideoPosterKey
        );
    }

    private void updatePublishedThreadContent(Long userId,
                                              WfForumThread thread,
                                              ThreadPayload payload,
                                              boolean publishFromDraft) {
        boolean changed = publishFromDraft
                || !payload.title.equals(thread.getTitle())
                || !payload.content.equals(thread.getContent())
                || !safeEquals(forumPayloadSupport.joinImageKeys(payload.imageKeys), thread.getImageKeys())
                || !safeEquals(payload.videoKey, thread.getVideoKey())
                || !safeEquals(payload.videoPosterKey, thread.getVideoPosterKey());

        if (!changed) {
            return;
        }

        LocalDateTime now = LocalDateTime.now();
        LambdaUpdateWrapper<WfForumThread> updateWrapper = new LambdaUpdateWrapper<>();
        updateWrapper.eq(WfForumThread::getId, thread.getId())
                .eq(WfForumThread::getAuthorId, userId)
                .eq(WfForumThread::getStatus, thread.getStatus())
                .set(WfForumThread::getTitle, payload.title)
                .set(WfForumThread::getContent, payload.content)
                .set(WfForumThread::getImageKeys, forumPayloadSupport.joinImageKeys(payload.imageKeys))
                .set(WfForumThread::getVideoKey, payload.videoKey)
                .set(WfForumThread::getVideoPosterKey, payload.videoPosterKey)
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

    private void validateEditableThreadStatus(ForumThreadStatus status) {
        if (status == null) {
            throw new BaseException(ErrorCode.FORUM_THREAD_NOT_FOUND);
        }
        if (!ForumThreadStatus.DRAFT.equals(status)
                && !ForumThreadStatus.NORMAL.equals(status)
                && !ForumThreadStatus.LOCKED.equals(status)
                && !ForumThreadStatus.DELETED.equals(status)) {
            throw new BaseException(ErrorCode.FORUM_THREAD_NOT_FOUND);
        }
    }

    private boolean isPublishedThreadStatus(ForumThreadStatus status) {
        return ForumThreadStatus.NORMAL.equals(status) || ForumThreadStatus.LOCKED.equals(status);
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

    private void restoreDeletedThreadContent(Long userId, WfForumThread thread, ThreadPayload payload) {
        LocalDateTime now = LocalDateTime.now();
        LambdaUpdateWrapper<WfForumThread> updateWrapper = new LambdaUpdateWrapper<>();
        updateWrapper.eq(WfForumThread::getId, thread.getId())
                .eq(WfForumThread::getAuthorId, userId)
                .eq(WfForumThread::getStatus, ForumThreadStatus.DELETED)
                .set(WfForumThread::getTitle, payload.title)
                .set(WfForumThread::getContent, payload.content)
                .set(WfForumThread::getImageKeys, forumPayloadSupport.joinImageKeys(payload.imageKeys))
                .set(WfForumThread::getVideoKey, payload.videoKey)
                .set(WfForumThread::getVideoPosterKey, payload.videoPosterKey)
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

    private static class ThreadPayload {
        private final String title;
        private final String content;
        private final List<String> imageKeys;
        private final String videoKey;
        private final String videoPosterKey;

        private ThreadPayload(String title,
                              String content,
                              List<String> imageKeys,
                              String videoKey,
                              String videoPosterKey) {
            this.title = title;
            this.content = content;
            this.imageKeys = imageKeys == null ? List.of() : new ArrayList<>(imageKeys);
            this.videoKey = videoKey;
            this.videoPosterKey = videoPosterKey;
        }
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
