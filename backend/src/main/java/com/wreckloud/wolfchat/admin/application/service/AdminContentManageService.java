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
import com.wreckloud.wolfchat.community.application.service.ForumContentMaintenanceService;
import com.wreckloud.wolfchat.community.application.service.ForumModerationLogService;
import com.wreckloud.wolfchat.community.domain.constant.ForumModerationLogConstants;
import com.wreckloud.wolfchat.community.domain.entity.WfForumReply;
import com.wreckloud.wolfchat.community.domain.entity.WfForumThread;
import com.wreckloud.wolfchat.community.domain.enums.ForumReplyStatus;
import com.wreckloud.wolfchat.community.domain.enums.ForumThreadStatus;
import com.wreckloud.wolfchat.community.domain.enums.ForumThreadType;
import com.wreckloud.wolfchat.community.infra.mapper.WfForumReplyMapper;
import com.wreckloud.wolfchat.community.infra.mapper.WfForumThreadMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

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

    private final WfForumThreadMapper wfForumThreadMapper;
    private final WfForumReplyMapper wfForumReplyMapper;
    private final ForumContentMaintenanceService forumContentMaintenanceService;
    private final ForumModerationLogService forumModerationLogService;
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

        String action = locked ? ForumModerationLogConstants.ACTION_LOCK_THREAD : ForumModerationLogConstants.ACTION_UNLOCK_THREAD;
        forumModerationLogService.record(
                operatorUserId,
                ForumModerationLogConstants.TARGET_THREAD,
                threadId,
                action,
                ForumModerationLogConstants.REASON_ADMIN_OPERATION
        );
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

        String action = sticky ? ForumModerationLogConstants.ACTION_STICKY_THREAD : ForumModerationLogConstants.ACTION_UNSTICKY_THREAD;
        forumModerationLogService.record(
                operatorUserId,
                ForumModerationLogConstants.TARGET_THREAD,
                threadId,
                action,
                ForumModerationLogConstants.REASON_ADMIN_OPERATION
        );
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

        String action = essence ? ForumModerationLogConstants.ACTION_ESSENCE_THREAD : ForumModerationLogConstants.ACTION_UNESSENCE_THREAD;
        forumModerationLogService.record(
                operatorUserId,
                ForumModerationLogConstants.TARGET_THREAD,
                threadId,
                action,
                ForumModerationLogConstants.REASON_ADMIN_OPERATION
        );
    }

    public void deleteThread(Long operatorUserId, Long threadId) {
        WfForumThread thread = getVisibleThreadOrThrow(threadId);

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

        forumContentMaintenanceService.refreshBoardStatsIfExists(thread.getBoardId());
        forumModerationLogService.record(
                operatorUserId,
                ForumModerationLogConstants.TARGET_THREAD,
                threadId,
                ForumModerationLogConstants.ACTION_DELETE_THREAD,
                ForumModerationLogConstants.REASON_ADMIN_OPERATION
        );
    }

    public void deleteReply(Long operatorUserId, Long replyId) {
        WfForumReply reply = getVisibleReplyOrThrow(replyId);
        WfForumThread thread = getVisibleThreadOrThrow(reply.getThreadId());

        LambdaUpdateWrapper<WfForumReply> replyUpdateWrapper = new LambdaUpdateWrapper<>();
        replyUpdateWrapper.eq(WfForumReply::getId, replyId)
                .eq(WfForumReply::getStatus, ForumReplyStatus.NORMAL)
                .set(WfForumReply::getStatus, ForumReplyStatus.DELETED);
        assertSingleRow(wfForumReplyMapper.update(null, replyUpdateWrapper), ErrorCode.FORUM_REPLY_NOT_FOUND);

        forumContentMaintenanceService.refreshThreadLastReply(thread.getId());
        forumContentMaintenanceService.refreshBoardStatsIfExists(thread.getBoardId());
        forumModerationLogService.record(
                operatorUserId,
                ForumModerationLogConstants.TARGET_REPLY,
                replyId,
                ForumModerationLogConstants.ACTION_DELETE_REPLY,
                ForumModerationLogConstants.REASON_ADMIN_OPERATION
        );
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
