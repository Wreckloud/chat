package com.wreckloud.wolfchat.admin.application.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.wreckloud.wolfchat.account.application.service.UserService;
import com.wreckloud.wolfchat.account.domain.entity.WfUser;
import com.wreckloud.wolfchat.admin.api.vo.AdminPageVO;
import com.wreckloud.wolfchat.admin.api.vo.AdminReplyRowVO;
import com.wreckloud.wolfchat.admin.api.vo.AdminThreadRowVO;
import com.wreckloud.wolfchat.common.excption.BaseException;
import com.wreckloud.wolfchat.common.excption.ErrorCode;
import com.wreckloud.wolfchat.community.domain.entity.WfForumBoard;
import com.wreckloud.wolfchat.community.domain.entity.WfForumModerationLog;
import com.wreckloud.wolfchat.community.domain.entity.WfForumReply;
import com.wreckloud.wolfchat.community.domain.entity.WfForumReplyLike;
import com.wreckloud.wolfchat.community.domain.entity.WfForumThread;
import com.wreckloud.wolfchat.community.domain.entity.WfForumThreadLike;
import com.wreckloud.wolfchat.community.domain.enums.ForumReplyStatus;
import com.wreckloud.wolfchat.community.domain.enums.ForumThreadStatus;
import com.wreckloud.wolfchat.community.domain.enums.ForumThreadType;
import com.wreckloud.wolfchat.community.infra.mapper.WfForumBoardMapper;
import com.wreckloud.wolfchat.community.infra.mapper.WfForumModerationLogMapper;
import com.wreckloud.wolfchat.community.infra.mapper.WfForumReplyLikeMapper;
import com.wreckloud.wolfchat.community.infra.mapper.WfForumReplyMapper;
import com.wreckloud.wolfchat.community.infra.mapper.WfForumThreadLikeMapper;
import com.wreckloud.wolfchat.community.infra.mapper.WfForumThreadMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * 管理端内容治理服务。
 */
@Service
@RequiredArgsConstructor
public class AdminContentManageService {
    private static final long MIN_PAGE = 1L;
    private static final long MIN_PAGE_SIZE = 1L;
    private static final long MAX_PAGE_SIZE = 100L;

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
    private static final String LOG_REASON_ADMIN_OPERATION = "ADMIN_OPERATION";

    private final WfForumThreadMapper wfForumThreadMapper;
    private final WfForumReplyMapper wfForumReplyMapper;
    private final WfForumThreadLikeMapper wfForumThreadLikeMapper;
    private final WfForumReplyLikeMapper wfForumReplyLikeMapper;
    private final WfForumBoardMapper wfForumBoardMapper;
    private final WfForumModerationLogMapper wfForumModerationLogMapper;
    private final UserService userService;

    public AdminPageVO<AdminThreadRowVO> listThreadPage(long page, long size) {
        validatePage(page, size);
        LambdaQueryWrapper<WfForumThread> queryWrapper = new LambdaQueryWrapper<>();
        queryWrapper.ne(WfForumThread::getStatus, ForumThreadStatus.DELETED)
                .orderByDesc(WfForumThread::getUpdateTime)
                .orderByDesc(WfForumThread::getId);

        Page<WfForumThread> result = wfForumThreadMapper.selectPage(new Page<>(page, size), queryWrapper);
        List<WfForumThread> records = result.getRecords();
        if (records.isEmpty()) {
            return AdminPageVO.of(result.getCurrent(), result.getSize(), result.getTotal(), Collections.emptyList());
        }

        Map<Long, WfUser> userMap = loadUserMapByIds(records.stream()
                .map(WfForumThread::getAuthorId)
                .collect(Collectors.toSet()));

        List<AdminThreadRowVO> list = new ArrayList<>(records.size());
        for (WfForumThread thread : records) {
            WfUser author = userMap.get(thread.getAuthorId());

            AdminThreadRowVO rowVO = new AdminThreadRowVO();
            rowVO.setThreadId(thread.getId());
            rowVO.setTitle(thread.getTitle());
            rowVO.setAuthorNickname(resolveNickname(author, thread.getAuthorId()));
            rowVO.setStatus(thread.getStatus());
            rowVO.setThreadType(thread.getThreadType());
            rowVO.setIsEssence(Boolean.TRUE.equals(thread.getIsEssence()));
            rowVO.setReplyCount(thread.getReplyCount());
            rowVO.setLikeCount(thread.getLikeCount());
            rowVO.setCreateTime(thread.getCreateTime());
            list.add(rowVO);
        }
        return AdminPageVO.of(result.getCurrent(), result.getSize(), result.getTotal(), list);
    }

    public AdminPageVO<AdminReplyRowVO> listReplyPage(long page, long size) {
        validatePage(page, size);
        LambdaQueryWrapper<WfForumReply> queryWrapper = new LambdaQueryWrapper<>();
        queryWrapper.eq(WfForumReply::getStatus, ForumReplyStatus.NORMAL)
                .orderByDesc(WfForumReply::getCreateTime)
                .orderByDesc(WfForumReply::getId);

        Page<WfForumReply> result = wfForumReplyMapper.selectPage(new Page<>(page, size), queryWrapper);
        List<WfForumReply> records = result.getRecords();
        if (records.isEmpty()) {
            return AdminPageVO.of(result.getCurrent(), result.getSize(), result.getTotal(), Collections.emptyList());
        }

        Map<Long, WfUser> userMap = loadUserMapByIds(records.stream()
                .map(WfForumReply::getAuthorId)
                .collect(Collectors.toSet()));

        List<AdminReplyRowVO> list = new ArrayList<>(records.size());
        for (WfForumReply reply : records) {
            WfUser author = userMap.get(reply.getAuthorId());

            AdminReplyRowVO rowVO = new AdminReplyRowVO();
            rowVO.setReplyId(reply.getId());
            rowVO.setThreadId(reply.getThreadId());
            rowVO.setAuthorNickname(resolveNickname(author, reply.getAuthorId()));
            rowVO.setContent(reply.getContent());
            rowVO.setLikeCount(reply.getLikeCount());
            rowVO.setCreateTime(reply.getCreateTime());
            list.add(rowVO);
        }
        return AdminPageVO.of(result.getCurrent(), result.getSize(), result.getTotal(), list);
    }

    public void updateThreadLockStatus(Long operatorUserId, Long threadId, boolean locked) {
        WfForumThread thread = getVisibleThreadOrThrow(threadId);
        ForumThreadStatus targetStatus = locked ? ForumThreadStatus.LOCKED : ForumThreadStatus.NORMAL;
        if (targetStatus.equals(thread.getStatus())) {
            return;
        }

        LambdaUpdateWrapper<WfForumThread> updateWrapper = new LambdaUpdateWrapper<>();
        updateWrapper.eq(WfForumThread::getId, threadId)
                .ne(WfForumThread::getStatus, ForumThreadStatus.DELETED)
                .set(WfForumThread::getStatus, targetStatus);
        assertSingleRow(wfForumThreadMapper.update(null, updateWrapper), ErrorCode.FORUM_THREAD_NOT_FOUND);

        String action = locked ? LOG_ACTION_LOCK_THREAD : LOG_ACTION_UNLOCK_THREAD;
        recordModerationLog(operatorUserId, LOG_TARGET_THREAD, threadId, action);
    }

    public void updateThreadStickyStatus(Long operatorUserId, Long threadId, boolean sticky) {
        WfForumThread thread = getVisibleThreadOrThrow(threadId);
        if (ForumThreadType.ANNOUNCEMENT.equals(thread.getThreadType())) {
            throw new BaseException(ErrorCode.PARAM_ERROR);
        }

        ForumThreadType targetType = sticky ? ForumThreadType.STICKY : ForumThreadType.NORMAL;
        if (targetType.equals(thread.getThreadType())) {
            return;
        }

        LambdaUpdateWrapper<WfForumThread> updateWrapper = new LambdaUpdateWrapper<>();
        updateWrapper.eq(WfForumThread::getId, threadId)
                .ne(WfForumThread::getStatus, ForumThreadStatus.DELETED)
                .set(WfForumThread::getThreadType, targetType);
        assertSingleRow(wfForumThreadMapper.update(null, updateWrapper), ErrorCode.FORUM_THREAD_NOT_FOUND);

        String action = sticky ? LOG_ACTION_STICKY_THREAD : LOG_ACTION_UNSTICKY_THREAD;
        recordModerationLog(operatorUserId, LOG_TARGET_THREAD, threadId, action);
    }

    public void updateThreadEssenceStatus(Long operatorUserId, Long threadId, boolean essence) {
        WfForumThread thread = getVisibleThreadOrThrow(threadId);
        if (Boolean.TRUE.equals(thread.getIsEssence()) == essence) {
            return;
        }

        LambdaUpdateWrapper<WfForumThread> updateWrapper = new LambdaUpdateWrapper<>();
        updateWrapper.eq(WfForumThread::getId, threadId)
                .ne(WfForumThread::getStatus, ForumThreadStatus.DELETED)
                .set(WfForumThread::getIsEssence, essence);
        assertSingleRow(wfForumThreadMapper.update(null, updateWrapper), ErrorCode.FORUM_THREAD_NOT_FOUND);

        String action = essence ? LOG_ACTION_ESSENCE_THREAD : LOG_ACTION_UNESSENCE_THREAD;
        recordModerationLog(operatorUserId, LOG_TARGET_THREAD, threadId, action);
    }

    public void deleteThread(Long operatorUserId, Long threadId) {
        WfForumThread thread = getVisibleThreadOrThrow(threadId);
        List<Long> replyIds = loadThreadReplyIds(threadId);

        LambdaUpdateWrapper<WfForumThread> threadUpdateWrapper = new LambdaUpdateWrapper<>();
        threadUpdateWrapper.eq(WfForumThread::getId, threadId)
                .ne(WfForumThread::getStatus, ForumThreadStatus.DELETED)
                .set(WfForumThread::getStatus, ForumThreadStatus.DELETED);
        assertSingleRow(wfForumThreadMapper.update(null, threadUpdateWrapper), ErrorCode.FORUM_THREAD_NOT_FOUND);

        LambdaUpdateWrapper<WfForumReply> replyUpdateWrapper = new LambdaUpdateWrapper<>();
        replyUpdateWrapper.eq(WfForumReply::getThreadId, threadId)
                .eq(WfForumReply::getStatus, ForumReplyStatus.NORMAL)
                .set(WfForumReply::getStatus, ForumReplyStatus.DELETED);
        wfForumReplyMapper.update(null, replyUpdateWrapper);

        deleteThreadLikesByThreadId(threadId);
        deleteReplyLikesByReplyIds(replyIds);
        refreshBoardStats(thread.getBoardId());
        recordModerationLog(operatorUserId, LOG_TARGET_THREAD, threadId, LOG_ACTION_DELETE_THREAD);
    }

    public void deleteReply(Long operatorUserId, Long replyId) {
        WfForumReply reply = getVisibleReplyOrThrow(replyId);
        WfForumThread thread = getVisibleThreadOrThrow(reply.getThreadId());

        LambdaUpdateWrapper<WfForumReply> replyUpdateWrapper = new LambdaUpdateWrapper<>();
        replyUpdateWrapper.eq(WfForumReply::getId, replyId)
                .eq(WfForumReply::getStatus, ForumReplyStatus.NORMAL)
                .set(WfForumReply::getStatus, ForumReplyStatus.DELETED);
        assertSingleRow(wfForumReplyMapper.update(null, replyUpdateWrapper), ErrorCode.FORUM_REPLY_NOT_FOUND);

        deleteReplyLikeByReplyId(replyId);

        LambdaUpdateWrapper<WfForumThread> threadUpdateWrapper = new LambdaUpdateWrapper<>();
        threadUpdateWrapper.eq(WfForumThread::getId, thread.getId())
                .ne(WfForumThread::getStatus, ForumThreadStatus.DELETED)
                .setSql("reply_count = IF(reply_count > 0, reply_count - 1, 0)");
        assertSingleRow(wfForumThreadMapper.update(null, threadUpdateWrapper), ErrorCode.FORUM_THREAD_NOT_FOUND);

        refreshThreadLastReply(thread.getId());
        refreshBoardStats(thread.getBoardId());
        recordModerationLog(operatorUserId, LOG_TARGET_REPLY, replyId, LOG_ACTION_DELETE_REPLY);
    }

    private WfForumThread getVisibleThreadOrThrow(Long threadId) {
        WfForumThread thread = wfForumThreadMapper.selectById(threadId);
        if (thread == null || ForumThreadStatus.DELETED.equals(thread.getStatus())) {
            throw new BaseException(ErrorCode.FORUM_THREAD_NOT_FOUND);
        }
        return thread;
    }

    private WfForumReply getVisibleReplyOrThrow(Long replyId) {
        WfForumReply reply = wfForumReplyMapper.selectById(replyId);
        if (reply == null || ForumReplyStatus.DELETED.equals(reply.getStatus())) {
            throw new BaseException(ErrorCode.FORUM_REPLY_NOT_FOUND);
        }
        return reply;
    }

    private List<Long> loadThreadReplyIds(Long threadId) {
        LambdaQueryWrapper<WfForumReply> queryWrapper = new LambdaQueryWrapper<>();
        queryWrapper.eq(WfForumReply::getThreadId, threadId)
                .select(WfForumReply::getId);
        List<WfForumReply> replies = wfForumReplyMapper.selectList(queryWrapper);
        if (replies == null || replies.isEmpty()) {
            return Collections.emptyList();
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
        WfForumBoard board = wfForumBoardMapper.selectById(boardId);
        if (board == null) {
            return;
        }

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

    private void recordModerationLog(Long operatorUserId, String targetType, Long targetId, String action) {
        if (operatorUserId == null) {
            throw new BaseException(ErrorCode.PARAM_ERROR);
        }

        WfForumModerationLog log = new WfForumModerationLog();
        log.setOperatorUserId(operatorUserId);
        log.setTargetType(targetType);
        log.setTargetId(targetId);
        log.setAction(action);
        log.setReason(LOG_REASON_ADMIN_OPERATION);
        assertSingleRow(wfForumModerationLogMapper.insert(log));
    }

    private Map<Long, WfUser> loadUserMapByIds(Set<Long> userIds) {
        if (userIds == null || userIds.isEmpty()) {
            return Collections.emptyMap();
        }
        Set<Long> uniqueIds = new HashSet<>(userIds);
        return userService.getUserMap(uniqueIds);
    }

    private String resolveNickname(WfUser user, Long userId) {
        if (user != null && user.getNickname() != null) {
            return user.getNickname();
        }
        return "用户#" + userId;
    }

    private void validatePage(long page, long size) {
        if (page < MIN_PAGE || size < MIN_PAGE_SIZE || size > MAX_PAGE_SIZE) {
            throw new BaseException(ErrorCode.PARAM_ERROR);
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

