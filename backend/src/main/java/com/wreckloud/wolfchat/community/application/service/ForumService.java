package com.wreckloud.wolfchat.community.application.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import com.wreckloud.wolfchat.account.application.service.UserAchievementService;
import com.wreckloud.wolfchat.common.excption.BaseException;
import com.wreckloud.wolfchat.common.excption.ErrorCode;
import com.wreckloud.wolfchat.community.api.dto.CreateReplyDTO;
import com.wreckloud.wolfchat.community.api.dto.CreateThreadDTO;
import com.wreckloud.wolfchat.community.api.vo.ForumBoardVO;
import com.wreckloud.wolfchat.community.api.vo.ForumReplyPageVO;
import com.wreckloud.wolfchat.community.api.vo.ForumReplyVO;
import com.wreckloud.wolfchat.community.api.vo.ForumThreadDetailVO;
import com.wreckloud.wolfchat.community.api.vo.ForumThreadPageVO;
import com.wreckloud.wolfchat.community.api.vo.ForumThreadVO;
import com.wreckloud.wolfchat.community.application.support.ForumPayloadSupport;
import com.wreckloud.wolfchat.community.domain.constant.ForumModerationLogConstants;
import com.wreckloud.wolfchat.community.domain.entity.WfForumBoard;
import com.wreckloud.wolfchat.community.domain.entity.WfForumReply;
import com.wreckloud.wolfchat.community.domain.entity.WfForumReplyLike;
import com.wreckloud.wolfchat.community.domain.entity.WfForumThread;
import com.wreckloud.wolfchat.community.domain.entity.WfForumThreadLike;
import com.wreckloud.wolfchat.community.domain.enums.ForumBoardStatus;
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
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;

/**
 * 论坛门面服务：读操作委托给查询服务，写操作在此统一编排。
 */
@Service
@RequiredArgsConstructor
public class ForumService {
    private static final int THREAD_IMAGE_MAX_COUNT = 9;

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
    private final UserNoticeService userNoticeService;

    public List<ForumBoardVO> listBoards() {
        return forumQueryService.listBoards();
    }

    public ForumThreadPageVO listBoardThreads(Long userId, Long boardId, long page, long size, String tab) {
        return forumQueryService.listBoardThreads(userId, boardId, page, size, tab);
    }

    @Transactional(rollbackFor = Exception.class)
    public ForumThreadVO createThread(Long userId, Long boardId, CreateThreadDTO dto) {
        WfForumBoard board = forumQueryService.getBoardOrThrow(boardId);
        if (ForumBoardStatus.CLOSED.equals(board.getStatus())) {
            throw new BaseException(ErrorCode.FORUM_BOARD_CLOSED);
        }

        String content = forumPayloadSupport.normalizeOptionalContent(dto.getContent());
        List<String> imageKeys = forumPayloadSupport.normalizeImageKeys(dto.getImageKeys(), THREAD_IMAGE_MAX_COUNT);
        String videoKey = forumPayloadSupport.normalizeOptionalKey(dto.getVideoKey());
        forumPayloadSupport.validateThreadPayload(userId, content, imageKeys, videoKey);

        LocalDateTime now = LocalDateTime.now();
        WfForumThread thread = new WfForumThread();
        thread.setBoardId(boardId);
        thread.setAuthorId(userId);
        thread.setTitle(dto.getTitle().trim());
        thread.setContent(content);
        thread.setImageKeys(forumPayloadSupport.joinImageKeys(imageKeys));
        thread.setVideoKey(videoKey);
        thread.setThreadType(ForumThreadType.NORMAL);
        thread.setStatus(ForumThreadStatus.NORMAL);
        thread.setIsEssence(false);
        thread.setViewCount(0);
        thread.setReplyCount(0);
        thread.setLastReplyTime(now);

        assertSingleRow(wfForumThreadMapper.insert(thread));
        updateBoardOnThreadCreate(boardId, thread.getId(), now);
        userAchievementService.grantFirstPostAchievement(userId);
        return forumQueryService.buildThreadVO(userId, thread.getId());
    }

    public ForumThreadDetailVO getThreadDetail(Long userId, Long threadId) {
        return forumQueryService.getThreadDetail(userId, threadId);
    }

    @Transactional(rollbackFor = Exception.class)
    public void increaseThreadView(Long threadId) {
        LambdaUpdateWrapper<WfForumThread> updateWrapper = new LambdaUpdateWrapper<>();
        updateWrapper.eq(WfForumThread::getId, threadId)
                .ne(WfForumThread::getStatus, ForumThreadStatus.DELETED)
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

    public ForumReplyPageVO listThreadReplies(Long userId, Long threadId, long page, long size) {
        return forumQueryService.listThreadReplies(userId, threadId, page, size);
    }

    @Transactional(rollbackFor = Exception.class)
    public ForumReplyVO createReply(Long userId, Long threadId, CreateReplyDTO dto) {
        WfForumThread thread = getThreadForReplyOrThrow(threadId);
        if (dto.getQuoteReplyId() != null) {
            forumQueryService.getQuoteReplyOrThrow(threadId, dto.getQuoteReplyId());
        }

        String content = forumPayloadSupport.normalizeOptionalContent(dto.getContent());
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
        reply.setQuoteReplyId(dto.getQuoteReplyId());
        reply.setStatus(ForumReplyStatus.NORMAL);

        assertSingleRow(wfForumReplyMapper.insert(reply));
        updateThreadOnReply(threadId, userId, reply.getId(), now);
        updateBoardOnReply(thread.getBoardId(), threadId, now);
        userAchievementService.grantFirstReplyAchievement(userId);
        userNoticeService.notifyThreadReplied(thread.getAuthorId(), thread.getId(), userId);
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

        List<Long> replyIds = forumContentMaintenanceService.loadThreadReplyIds(threadId);

        LambdaUpdateWrapper<WfForumThread> threadUpdateWrapper = new LambdaUpdateWrapper<>();
        threadUpdateWrapper.eq(WfForumThread::getId, threadId)
                .eq(WfForumThread::getAuthorId, userId)
                .ne(WfForumThread::getStatus, ForumThreadStatus.DELETED)
                .set(WfForumThread::getStatus, ForumThreadStatus.DELETED);
        assertSingleRow(wfForumThreadMapper.update(null, threadUpdateWrapper));

        LambdaUpdateWrapper<WfForumReply> replyUpdateWrapper = new LambdaUpdateWrapper<>();
        replyUpdateWrapper.eq(WfForumReply::getThreadId, threadId)
                .eq(WfForumReply::getStatus, ForumReplyStatus.NORMAL)
                .set(WfForumReply::getStatus, ForumReplyStatus.DELETED);
        wfForumReplyMapper.update(null, replyUpdateWrapper);

        forumContentMaintenanceService.deleteThreadLikesByThreadId(threadId);
        forumContentMaintenanceService.deleteReplyLikesByReplyIds(replyIds);

        forumContentMaintenanceService.refreshBoardStatsOrThrow(thread.getBoardId());
        recordThreadAuthorAction(userId, threadId, ForumModerationLogConstants.ACTION_DELETE_THREAD);
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

        forumContentMaintenanceService.deleteReplyLikeByReplyId(replyId);

        LambdaUpdateWrapper<WfForumThread> threadUpdateWrapper = new LambdaUpdateWrapper<>();
        threadUpdateWrapper.eq(WfForumThread::getId, thread.getId())
                .ne(WfForumThread::getStatus, ForumThreadStatus.DELETED)
                .setSql("reply_count = IF(reply_count > 0, reply_count - 1, 0)");
        assertSingleRow(wfForumThreadMapper.update(null, threadUpdateWrapper));

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
                .ne(WfForumThread::getStatus, ForumThreadStatus.DELETED)
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
                .ne(WfForumThread::getStatus, ForumThreadStatus.DELETED)
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
                .ne(WfForumThread::getStatus, ForumThreadStatus.DELETED);
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
