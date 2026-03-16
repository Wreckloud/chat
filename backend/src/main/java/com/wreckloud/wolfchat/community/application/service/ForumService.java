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
import com.wreckloud.wolfchat.community.domain.entity.WfForumBoard;
import com.wreckloud.wolfchat.community.domain.entity.WfForumModerationLog;
import com.wreckloud.wolfchat.community.domain.entity.WfForumReply;
import com.wreckloud.wolfchat.community.domain.entity.WfForumReplyLike;
import com.wreckloud.wolfchat.community.domain.entity.WfForumThread;
import com.wreckloud.wolfchat.community.domain.entity.WfForumThreadLike;
import com.wreckloud.wolfchat.community.domain.enums.ForumBoardStatus;
import com.wreckloud.wolfchat.community.domain.enums.ForumReplyStatus;
import com.wreckloud.wolfchat.community.domain.enums.ForumThreadStatus;
import com.wreckloud.wolfchat.community.domain.enums.ForumThreadType;
import com.wreckloud.wolfchat.community.infra.mapper.WfForumBoardMapper;
import com.wreckloud.wolfchat.community.infra.mapper.WfForumModerationLogMapper;
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
import java.util.Map;
import java.util.stream.Collectors;

/**
 * 论坛门面服务：读操作委托给查询服务，写操作在此统一编排。
 */
@Service
@RequiredArgsConstructor
public class ForumService {
    private static final String LOG_TARGET_THREAD = "THREAD";
    private static final String LOG_TARGET_REPLY = "REPLY";
    private static final String LOG_ACTION_LOCK_THREAD = "LOCK_THREAD";
    private static final String LOG_ACTION_UNLOCK_THREAD = "UNLOCK_THREAD";
    private static final String LOG_ACTION_STICKY_THREAD = "STICKY_THREAD";
    private static final String LOG_ACTION_UNSTICKY_THREAD = "UNSTICKY_THREAD";
    private static final String LOG_ACTION_ESSENCE_THREAD = "ESSENCE_THREAD";
    private static final String LOG_ACTION_UNESSENCE_THREAD = "UNESSENCE_THREAD";
    private static final String LOG_ACTION_DELETE_THREAD = "DELETE_THREAD";
    private static final String LOG_ACTION_DELETE_REPLY = "DELETE_REPLY";
    private static final String LOG_REASON_AUTHOR_OPERATION = "AUTHOR_OPERATION";

    private final ForumQueryService forumQueryService;
    private final WfForumBoardMapper wfForumBoardMapper;
    private final WfForumThreadMapper wfForumThreadMapper;
    private final WfForumReplyMapper wfForumReplyMapper;
    private final WfForumThreadLikeMapper wfForumThreadLikeMapper;
    private final WfForumReplyLikeMapper wfForumReplyLikeMapper;
    private final WfForumModerationLogMapper wfForumModerationLogMapper;
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

        LocalDateTime now = LocalDateTime.now();
        WfForumThread thread = new WfForumThread();
        thread.setBoardId(boardId);
        thread.setAuthorId(userId);
        thread.setTitle(dto.getTitle().trim());
        thread.setContent(dto.getContent().trim());
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

        int floorNo = getNextFloorNo(threadId);
        LocalDateTime now = LocalDateTime.now();

        WfForumReply reply = new WfForumReply();
        reply.setThreadId(threadId);
        reply.setFloorNo(floorNo);
        reply.setAuthorId(userId);
        reply.setContent(dto.getContent().trim());
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

        LambdaUpdateWrapper<WfForumThread> updateWrapper = new LambdaUpdateWrapper<>();
        updateWrapper.eq(WfForumThread::getId, threadId)
                .eq(WfForumThread::getAuthorId, userId)
                .eq(WfForumThread::getStatus, thread.getStatus())
                .set(WfForumThread::getStatus, targetStatus);
        assertSingleRow(wfForumThreadMapper.update(null, updateWrapper));

        String action = locked ? LOG_ACTION_LOCK_THREAD : LOG_ACTION_UNLOCK_THREAD;
        recordModerationLog(userId, LOG_TARGET_THREAD, threadId, action, LOG_REASON_AUTHOR_OPERATION);
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

        LambdaUpdateWrapper<WfForumThread> updateWrapper = new LambdaUpdateWrapper<>();
        updateWrapper.eq(WfForumThread::getId, threadId)
                .eq(WfForumThread::getAuthorId, userId)
                .eq(WfForumThread::getThreadType, thread.getThreadType())
                .ne(WfForumThread::getStatus, ForumThreadStatus.DELETED)
                .set(WfForumThread::getThreadType, targetType);
        assertSingleRow(wfForumThreadMapper.update(null, updateWrapper));

        String action = sticky ? LOG_ACTION_STICKY_THREAD : LOG_ACTION_UNSTICKY_THREAD;
        recordModerationLog(userId, LOG_TARGET_THREAD, threadId, action, LOG_REASON_AUTHOR_OPERATION);
    }

    @Transactional(rollbackFor = Exception.class)
    public void updateThreadEssenceStatus(Long userId, Long threadId, boolean essence) {
        WfForumThread thread = forumQueryService.getVisibleThreadOrThrow(threadId);
        validateThreadAuthor(userId, thread);

        boolean currentEssence = Boolean.TRUE.equals(thread.getIsEssence());
        if (currentEssence == essence) {
            return;
        }

        LambdaUpdateWrapper<WfForumThread> updateWrapper = new LambdaUpdateWrapper<>();
        updateWrapper.eq(WfForumThread::getId, threadId)
                .eq(WfForumThread::getAuthorId, userId)
                .ne(WfForumThread::getStatus, ForumThreadStatus.DELETED)
                .set(WfForumThread::getIsEssence, essence);
        assertSingleRow(wfForumThreadMapper.update(null, updateWrapper));

        String action = essence ? LOG_ACTION_ESSENCE_THREAD : LOG_ACTION_UNESSENCE_THREAD;
        recordModerationLog(userId, LOG_TARGET_THREAD, threadId, action, LOG_REASON_AUTHOR_OPERATION);
    }

    @Transactional(rollbackFor = Exception.class)
    public void deleteThread(Long userId, Long threadId) {
        WfForumThread thread = forumQueryService.getVisibleThreadOrThrow(threadId);
        validateThreadAuthor(userId, thread);

        List<Long> replyIds = loadThreadReplyIds(threadId);

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

        deleteThreadLikesByThreadId(threadId);
        deleteReplyLikesByReplyIds(replyIds);

        refreshBoardStats(thread.getBoardId());
        recordModerationLog(userId, LOG_TARGET_THREAD, threadId, LOG_ACTION_DELETE_THREAD, LOG_REASON_AUTHOR_OPERATION);
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

        deleteReplyLikeByReplyId(replyId);

        LambdaUpdateWrapper<WfForumThread> threadUpdateWrapper = new LambdaUpdateWrapper<>();
        threadUpdateWrapper.eq(WfForumThread::getId, thread.getId())
                .ne(WfForumThread::getStatus, ForumThreadStatus.DELETED)
                .setSql("reply_count = IF(reply_count > 0, reply_count - 1, 0)");
        assertSingleRow(wfForumThreadMapper.update(null, threadUpdateWrapper));

        refreshThreadLastReply(thread.getId());
        refreshBoardStats(thread.getBoardId());
        recordModerationLog(userId, LOG_TARGET_REPLY, replyId, LOG_ACTION_DELETE_REPLY, LOG_REASON_AUTHOR_OPERATION);
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
        return wfForumThreadLikeMapper.selectOne(queryWrapper) != null;
    }

    private boolean existsReplyLike(Long userId, Long replyId) {
        LambdaQueryWrapper<WfForumReplyLike> queryWrapper = new LambdaQueryWrapper<>();
        queryWrapper.eq(WfForumReplyLike::getUserId, userId)
                .eq(WfForumReplyLike::getReplyId, replyId)
                .last("LIMIT 1");
        return wfForumReplyLikeMapper.selectOne(queryWrapper) != null;
    }

    private List<Long> loadThreadReplyIds(Long threadId) {
        LambdaQueryWrapper<WfForumReply> queryWrapper = new LambdaQueryWrapper<>();
        queryWrapper.eq(WfForumReply::getThreadId, threadId)
                .select(WfForumReply::getId);
        List<WfForumReply> replies = wfForumReplyMapper.selectList(queryWrapper);
        if (replies == null || replies.isEmpty()) {
            return List.of();
        }
        return replies.stream().map(WfForumReply::getId).collect(Collectors.toList());
    }

    private void deleteThreadLikesByThreadId(Long threadId) {
        LambdaQueryWrapper<WfForumThreadLike> deleteWrapper = new LambdaQueryWrapper<>();
        deleteWrapper.eq(WfForumThreadLike::getThreadId, threadId);
        wfForumThreadLikeMapper.delete(deleteWrapper);
    }

    private void deleteReplyLikesByReplyIds(List<Long> replyIds) {
        if (replyIds == null || replyIds.isEmpty()) {
            return;
        }
        LambdaQueryWrapper<WfForumReplyLike> deleteWrapper = new LambdaQueryWrapper<>();
        deleteWrapper.in(WfForumReplyLike::getReplyId, replyIds);
        wfForumReplyLikeMapper.delete(deleteWrapper);
    }

    private void deleteReplyLikeByReplyId(Long replyId) {
        LambdaQueryWrapper<WfForumReplyLike> deleteWrapper = new LambdaQueryWrapper<>();
        deleteWrapper.eq(WfForumReplyLike::getReplyId, replyId);
        wfForumReplyLikeMapper.delete(deleteWrapper);
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

    private void updateBoardOnThreadCreate(Long boardId, Long threadId, LocalDateTime now) {
        LambdaUpdateWrapper<WfForumBoard> updateWrapper = new LambdaUpdateWrapper<>();
        updateWrapper.eq(WfForumBoard::getId, boardId)
                .setSql("thread_count = thread_count + 1")
                .set(WfForumBoard::getLastThreadId, threadId)
                .set(WfForumBoard::getLastReplyTime, now);
        assertSingleRow(wfForumBoardMapper.update(null, updateWrapper));
    }

    private void refreshThreadLastReply(Long threadId) {
        WfForumThread thread = wfForumThreadMapper.selectById(threadId);
        if (thread == null || ForumThreadStatus.DELETED.equals(thread.getStatus())) {
            throw new BaseException(ErrorCode.FORUM_THREAD_NOT_FOUND);
        }

        LambdaQueryWrapper<WfForumReply> queryWrapper = new LambdaQueryWrapper<>();
        queryWrapper.eq(WfForumReply::getThreadId, threadId)
                .eq(WfForumReply::getStatus, ForumReplyStatus.NORMAL)
                .orderByDesc(WfForumReply::getFloorNo)
                .last("LIMIT 1");
        WfForumReply lastReply = wfForumReplyMapper.selectOne(queryWrapper);

        LambdaUpdateWrapper<WfForumThread> updateWrapper = new LambdaUpdateWrapper<>();
        updateWrapper.eq(WfForumThread::getId, threadId);
        if (lastReply == null) {
            updateWrapper.set(WfForumThread::getLastReplyId, null)
                    .set(WfForumThread::getLastReplyUserId, thread.getAuthorId())
                    .set(WfForumThread::getLastReplyTime, thread.getCreateTime());
        } else {
            updateWrapper.set(WfForumThread::getLastReplyId, lastReply.getId())
                    .set(WfForumThread::getLastReplyUserId, lastReply.getAuthorId())
                    .set(WfForumThread::getLastReplyTime, lastReply.getCreateTime());
        }
        assertSingleRow(wfForumThreadMapper.update(null, updateWrapper));
    }

    private void refreshBoardStats(Long boardId) {
        Map<String, Object> boardStats = wfForumThreadMapper.selectBoardStats(boardId);
        int threadCount = parseStatsCount(boardStats, "threadCount");
        int replyCount = parseStatsCount(boardStats, "replyCount");
        WfForumThread lastThread = wfForumThreadMapper.selectLatestVisibleByBoardId(boardId);

        LambdaUpdateWrapper<WfForumBoard> boardUpdateWrapper = new LambdaUpdateWrapper<>();
        boardUpdateWrapper.eq(WfForumBoard::getId, boardId)
                .set(WfForumBoard::getThreadCount, threadCount)
                .set(WfForumBoard::getReplyCount, replyCount);
        if (lastThread == null) {
            boardUpdateWrapper.set(WfForumBoard::getLastThreadId, null)
                    .set(WfForumBoard::getLastReplyTime, null);
        } else {
            boardUpdateWrapper.set(WfForumBoard::getLastThreadId, lastThread.getId())
                    .set(WfForumBoard::getLastReplyTime, lastThread.getLastReplyTime());
        }
        assertSingleRow(wfForumBoardMapper.update(null, boardUpdateWrapper));
    }

    private void recordModerationLog(Long operatorUserId, String targetType, Long targetId, String action, String reason) {
        WfForumModerationLog log = new WfForumModerationLog();
        log.setOperatorUserId(operatorUserId);
        log.setTargetType(targetType);
        log.setTargetId(targetId);
        log.setAction(action);
        log.setReason(reason);
        assertSingleRow(wfForumModerationLogMapper.insert(log));
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

    private int parseStatsCount(Map<String, Object> boardStats, String key) {
        if (boardStats == null) {
            return 0;
        }
        Object value = boardStats.get(key);
        if (!(value instanceof Number)) {
            return 0;
        }
        int count = ((Number) value).intValue();
        return Math.max(count, 0);
    }
}
