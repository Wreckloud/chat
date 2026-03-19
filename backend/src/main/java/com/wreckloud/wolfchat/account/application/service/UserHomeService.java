package com.wreckloud.wolfchat.account.application.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.wreckloud.wolfchat.account.api.converter.UserConverter;
import com.wreckloud.wolfchat.account.api.vo.UserHomeThreadPageVO;
import com.wreckloud.wolfchat.account.api.vo.UserHomeThreadVO;
import com.wreckloud.wolfchat.account.api.vo.UserHomeVO;
import com.wreckloud.wolfchat.account.domain.entity.WfUser;
import com.wreckloud.wolfchat.common.excption.BaseException;
import com.wreckloud.wolfchat.common.excption.ErrorCode;
import com.wreckloud.wolfchat.community.domain.entity.WfForumThread;
import com.wreckloud.wolfchat.community.domain.enums.ForumThreadStatus;
import com.wreckloud.wolfchat.community.infra.mapper.WfForumReplyMapper;
import com.wreckloud.wolfchat.community.infra.mapper.WfForumThreadMapper;
import com.wreckloud.wolfchat.follow.domain.entity.WfFollow;
import com.wreckloud.wolfchat.follow.domain.enums.FollowStatus;
import com.wreckloud.wolfchat.follow.infra.mapper.WfFollowMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;

/**
 * @Description 行者主页服务
 * @Author Wreckloud
 * @Date 2026-03-16
 */
@Service
@RequiredArgsConstructor
public class UserHomeService {
    private static final long MIN_PAGE = 1L;
    private static final long MIN_SIZE = 1L;
    private static final long MAX_SIZE = 50L;
    private static final int HOME_LATEST_THREAD_LIMIT = 3;

    private final UserService userService;
    private final WfFollowMapper wfFollowMapper;
    private final WfForumThreadMapper wfForumThreadMapper;
    private final WfForumReplyMapper wfForumReplyMapper;
    private final UserAchievementService userAchievementService;

    /**
     * 获取行者主页
     */
    public UserHomeVO getUserHome(Long currentUserId, Long targetUserId) {
        if (currentUserId == null || targetUserId == null) {
            throw new BaseException(ErrorCode.PARAM_ERROR);
        }

        WfUser targetUser = userService.getEnabledByIdOrThrow(targetUserId);
        boolean self = currentUserId.equals(targetUserId);
        boolean following = !self && isFollowing(currentUserId, targetUserId);
        boolean mutual = following && isFollowing(targetUserId, currentUserId);

        UserHomeVO vo = new UserHomeVO();
        vo.setUser(UserConverter.toUserPublicVO(targetUser));
        vo.setSelf(self);
        vo.setFollowing(following);
        vo.setMutual(mutual);
        vo.setActiveDayCount(toSafeInt(targetUser.getActiveDayCount()));
        vo.setFollowerCount(countFollowers(targetUserId));
        vo.setFollowingCount(countFollowing(targetUserId));
        vo.setThreadCount(countVisibleThreads(targetUserId));
        vo.setReplyCount(toSafeInt(wfForumReplyMapper.selectVisibleCountByAuthorId(targetUserId)));
        vo.setTotalLikeCount(calculateTotalLikeCount(targetUserId));
        vo.setLastActiveAt(targetUser.getLastLoginAt());
        vo.setShowcaseTitles(userAchievementService.listUserTitleShowcase(targetUserId));
        vo.setLatestThreads(listLatestThreads(targetUserId, HOME_LATEST_THREAD_LIMIT));
        return vo;
    }

    /**
     * 分页获取指定行者发布的主题
     */
    public UserHomeThreadPageVO listUserThreads(Long targetUserId, long page, long size) {
        if (targetUserId == null || page < MIN_PAGE || size < MIN_SIZE || size > MAX_SIZE) {
            throw new BaseException(ErrorCode.PARAM_ERROR);
        }
        userService.getEnabledByIdOrThrow(targetUserId);

        LambdaQueryWrapper<WfForumThread> queryWrapper = new LambdaQueryWrapper<>();
        queryWrapper.eq(WfForumThread::getAuthorId, targetUserId)
                .ne(WfForumThread::getStatus, ForumThreadStatus.DELETED)
                .orderByDesc(WfForumThread::getCreateTime)
                .orderByDesc(WfForumThread::getId);

        Page<WfForumThread> pageReq = new Page<>(page, size);
        Page<WfForumThread> result = wfForumThreadMapper.selectPage(pageReq, queryWrapper);

        UserHomeThreadPageVO pageVO = new UserHomeThreadPageVO();
        pageVO.setList(mapThreads(result.getRecords()));
        pageVO.setTotal(result.getTotal());
        pageVO.setPage(page);
        pageVO.setSize(size);
        return pageVO;
    }

    private List<UserHomeThreadVO> listLatestThreads(Long targetUserId, int limit) {
        LambdaQueryWrapper<WfForumThread> queryWrapper = new LambdaQueryWrapper<>();
        queryWrapper.eq(WfForumThread::getAuthorId, targetUserId)
                .ne(WfForumThread::getStatus, ForumThreadStatus.DELETED)
                .orderByDesc(WfForumThread::getCreateTime)
                .orderByDesc(WfForumThread::getId)
                .last("LIMIT " + limit);
        List<WfForumThread> threads = wfForumThreadMapper.selectList(queryWrapper);
        return mapThreads(threads);
    }

    private List<UserHomeThreadVO> mapThreads(List<WfForumThread> threads) {
        if (threads == null || threads.isEmpty()) {
            return Collections.emptyList();
        }

        return threads.stream().map(thread -> {
            UserHomeThreadVO vo = new UserHomeThreadVO();
            vo.setThreadId(thread.getId());
            vo.setTitle(thread.getTitle());
            vo.setThreadType(thread.getThreadType());
            vo.setStatus(thread.getStatus());
            vo.setIsEssence(Boolean.TRUE.equals(thread.getIsEssence()));
            vo.setViewCount(toSafeInt(thread.getViewCount()));
            vo.setReplyCount(toSafeInt(thread.getReplyCount()));
            vo.setLikeCount(toSafeInt(thread.getLikeCount()));
            vo.setLastReplyTime(thread.getLastReplyTime());
            vo.setCreateTime(thread.getCreateTime());
            return vo;
        }).collect(Collectors.toList());
    }

    private boolean isFollowing(Long followerId, Long followeeId) {
        LambdaQueryWrapper<WfFollow> queryWrapper = new LambdaQueryWrapper<>();
        queryWrapper.eq(WfFollow::getFollowerId, followerId)
                .eq(WfFollow::getFolloweeId, followeeId)
                .eq(WfFollow::getStatus, FollowStatus.FOLLOWING)
                .last("LIMIT 1");
        return wfFollowMapper.selectOne(queryWrapper) != null;
    }

    private int countFollowers(Long userId) {
        LambdaQueryWrapper<WfFollow> queryWrapper = new LambdaQueryWrapper<>();
        queryWrapper.eq(WfFollow::getFolloweeId, userId)
                .eq(WfFollow::getStatus, FollowStatus.FOLLOWING);
        return toSafeInt(wfFollowMapper.selectCount(queryWrapper));
    }

    private int countFollowing(Long userId) {
        LambdaQueryWrapper<WfFollow> queryWrapper = new LambdaQueryWrapper<>();
        queryWrapper.eq(WfFollow::getFollowerId, userId)
                .eq(WfFollow::getStatus, FollowStatus.FOLLOWING);
        return toSafeInt(wfFollowMapper.selectCount(queryWrapper));
    }

    private int countVisibleThreads(Long userId) {
        LambdaQueryWrapper<WfForumThread> queryWrapper = new LambdaQueryWrapper<>();
        queryWrapper.eq(WfForumThread::getAuthorId, userId)
                .ne(WfForumThread::getStatus, ForumThreadStatus.DELETED);
        return toSafeInt(wfForumThreadMapper.selectCount(queryWrapper));
    }

    private int calculateTotalLikeCount(Long userId) {
        long threadLikeCount = toSafeLong(wfForumThreadMapper.selectLikeCountSumByAuthorId(userId));
        long replyLikeCount = toSafeLong(wfForumReplyMapper.selectLikeCountSumByAuthorId(userId));
        long total = threadLikeCount + replyLikeCount;
        if (total >= Integer.MAX_VALUE) {
            return Integer.MAX_VALUE;
        }
        return (int) Math.max(total, 0);
    }

    private int toSafeInt(Long value) {
        if (value == null || value <= 0L) {
            return 0;
        }
        if (value >= Integer.MAX_VALUE) {
            return Integer.MAX_VALUE;
        }
        return value.intValue();
    }

    private int toSafeInt(Integer value) {
        if (value == null || value <= 0) {
            return 0;
        }
        return value;
    }

    private long toSafeLong(Long value) {
        if (value == null || value <= 0L) {
            return 0L;
        }
        return value;
    }
}
