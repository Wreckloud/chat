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
import com.wreckloud.wolfchat.common.storage.service.MediaStorageService;
import com.wreckloud.wolfchat.community.domain.entity.WfForumThread;
import com.wreckloud.wolfchat.community.domain.enums.ForumThreadStatus;
import com.wreckloud.wolfchat.community.infra.mapper.WfForumReplyMapper;
import com.wreckloud.wolfchat.community.infra.mapper.WfForumThreadMapper;
import com.wreckloud.wolfchat.follow.domain.entity.WfFollow;
import com.wreckloud.wolfchat.follow.domain.enums.FollowStatus;
import com.wreckloud.wolfchat.follow.infra.mapper.WfFollowMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import java.util.Arrays;
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
    private static final int THREAD_CONTENT_PREVIEW_MAX_LENGTH = 90;
    private static final String THREAD_TAB_MINE = "mine";
    private static final String THREAD_TAB_DRAFT = "draft";
    private static final String THREAD_TAB_TRASH = "trash";
    private static final int THREAD_SEARCH_KEYWORD_MAX_LENGTH = 40;

    private final UserService userService;
    private final WfFollowMapper wfFollowMapper;
    private final WfForumThreadMapper wfForumThreadMapper;
    private final WfForumReplyMapper wfForumReplyMapper;
    private final UserAchievementService userAchievementService;
    private final MediaStorageService mediaStorageService;

    /**
     * 获取行者主页
     */
    public UserHomeVO getUserHome(Long currentUserId, Long targetUserId) {
        if (currentUserId == null || targetUserId == null) {
            throw new BaseException(ErrorCode.PARAM_ERROR);
        }

        // 主页允许查看已注销用户的历史资料快照；写操作仍在各自服务内校验可用状态。
        WfUser targetUser = userService.getByIdOrThrow(targetUserId);
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
        // 已注销用户的历史帖子可查看（只读）；不存在用户仍按统一错误返回。
        userService.getByIdOrThrow(targetUserId);

        LambdaQueryWrapper<WfForumThread> queryWrapper = new LambdaQueryWrapper<>();
        queryWrapper.eq(WfForumThread::getAuthorId, targetUserId)
                .in(WfForumThread::getStatus, List.of(ForumThreadStatus.NORMAL, ForumThreadStatus.LOCKED))
                .orderByDesc(WfForumThread::getThreadType)
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

    /**
     * 分页获取当前登录行者的主题（我的/草稿/垃圾站）
     */
    public UserHomeThreadPageVO listMyThreads(Long userId, String tab, String keyword, long page, long size) {
        if (userId == null || page < MIN_PAGE || size < MIN_SIZE || size > MAX_SIZE) {
            throw new BaseException(ErrorCode.PARAM_ERROR);
        }
        userService.getByIdOrThrow(userId);

        String normalizedTab = normalizeThreadTab(tab);
        String normalizedKeyword = normalizeThreadKeyword(keyword);

        LambdaQueryWrapper<WfForumThread> queryWrapper = new LambdaQueryWrapper<>();
        queryWrapper.eq(WfForumThread::getAuthorId, userId);
        applyThreadTabFilter(queryWrapper, normalizedTab);
        if (StringUtils.hasText(normalizedKeyword)) {
            queryWrapper.and(wrapper -> wrapper.like(WfForumThread::getTitle, normalizedKeyword)
                    .or()
                    .like(WfForumThread::getContent, normalizedKeyword));
        }
        queryWrapper.orderByDesc(WfForumThread::getUpdateTime)
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
                .in(WfForumThread::getStatus, List.of(ForumThreadStatus.NORMAL, ForumThreadStatus.LOCKED))
                .orderByDesc(WfForumThread::getThreadType)
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
            vo.setContentPreview(buildContentPreview(thread.getContent()));
            vo.setImageUrls(resolveImageUrls(thread.getImageKeys()));
            vo.setVideoPosterUrl(resolveVideoPosterUrl(thread));
            vo.setLastReplyTime(thread.getLastReplyTime());
            vo.setCreateTime(thread.getCreateTime());
            vo.setEditTime(thread.getEditTime());
            return vo;
        }).collect(Collectors.toList());
    }

    private List<String> resolveImageUrls(String imageKeys) {
        if (!StringUtils.hasText(imageKeys)) {
            return List.of();
        }
        return Arrays.stream(imageKeys.split(","))
                .map(String::trim)
                .filter(StringUtils::hasText)
                .map(this::resolveMediaUrl)
                .filter(StringUtils::hasText)
                .collect(Collectors.toList());
    }

    private String resolveVideoPosterUrl(WfForumThread thread) {
        if (thread == null || !StringUtils.hasText(thread.getVideoPosterKey())) {
            return null;
        }
        return resolveMediaUrl(thread.getVideoPosterKey());
    }

    private String resolveMediaUrl(String mediaKey) {
        if (!StringUtils.hasText(mediaKey)) {
            return null;
        }
        return mediaStorageService.buildSignedReadUrl(mediaKey.trim());
    }

    private String buildContentPreview(String content) {
        if (!StringUtils.hasText(content)) {
            return "";
        }
        String normalized = content.replaceAll("\\s+", " ").trim();
        if (normalized.length() <= THREAD_CONTENT_PREVIEW_MAX_LENGTH) {
            return normalized;
        }
        return normalized.substring(0, THREAD_CONTENT_PREVIEW_MAX_LENGTH) + "...";
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
                .in(WfForumThread::getStatus, List.of(ForumThreadStatus.NORMAL, ForumThreadStatus.LOCKED));
        return toSafeInt(wfForumThreadMapper.selectCount(queryWrapper));
    }

    private String normalizeThreadTab(String tab) {
        if (!StringUtils.hasText(tab)) {
            return THREAD_TAB_MINE;
        }
        String normalized = tab.trim().toLowerCase();
        if (THREAD_TAB_MINE.equals(normalized)
                || THREAD_TAB_DRAFT.equals(normalized)
                || THREAD_TAB_TRASH.equals(normalized)) {
            return normalized;
        }
        throw new BaseException(ErrorCode.PARAM_ERROR, "tab 参数不合法，仅支持 mine/draft/trash");
    }

    private String normalizeThreadKeyword(String keyword) {
        if (!StringUtils.hasText(keyword)) {
            return "";
        }
        String normalized = keyword.trim();
        if (normalized.length() > THREAD_SEARCH_KEYWORD_MAX_LENGTH) {
            throw new BaseException(ErrorCode.PARAM_ERROR, "关键词长度不能超过40个字符");
        }
        return normalized;
    }

    private void applyThreadTabFilter(LambdaQueryWrapper<WfForumThread> queryWrapper, String tab) {
        if (THREAD_TAB_DRAFT.equals(tab)) {
            queryWrapper.eq(WfForumThread::getStatus, ForumThreadStatus.DRAFT);
            return;
        }
        if (THREAD_TAB_TRASH.equals(tab)) {
            queryWrapper.eq(WfForumThread::getStatus, ForumThreadStatus.DELETED);
            return;
        }
        queryWrapper.in(WfForumThread::getStatus, List.of(ForumThreadStatus.NORMAL, ForumThreadStatus.LOCKED));
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
